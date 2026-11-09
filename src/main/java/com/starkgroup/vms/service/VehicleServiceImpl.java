package com.starkgroup.vms.service;

import com.starkgroup.vms.domain.Vehicle;
import com.starkgroup.vms.dto.CreateVehicleRequest;
import com.starkgroup.vms.dto.UpdateVehicleRequest;
import com.starkgroup.vms.dto.VehicleResponse;
import com.starkgroup.vms.exception.DuplicateVehicleNumberException;
import com.starkgroup.vms.exception.VehicleNotFoundException;
import com.starkgroup.vms.repository.VehicleRepository;
import com.starkgroup.vms.util.SecurityUtils;
import com.starkgroup.vms.util.VehicleNumberNormalizer;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Validated
public class VehicleServiceImpl implements VehicleService {

    private static final String DUPLICATE_VEHICLE_NUMBER_MESSAGE =
            "A vehicle with this Vehicle Number already exists.";

    private final VehicleRepository vehicleRepository;
    private final Validator validator;

    public VehicleServiceImpl(VehicleRepository vehicleRepository) {
        this(vehicleRepository, Validation.buildDefaultValidatorFactory().getValidator());
    }

    @Autowired
    public VehicleServiceImpl(VehicleRepository vehicleRepository, Validator validator) {
        this.vehicleRepository = vehicleRepository;
        this.validator = validator != null ? validator : Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Override
    @Transactional
    public VehicleResponse createVehicle(CreateVehicleRequest request) {
        validate(request);

        String normalizedNumber = VehicleNumberNormalizer.normalize(request.vehicleNumber());
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();

        // 1. Duplicate check (scoped to user, with global fallback)
        if (vehicleRepository.existsByVehicleNumberAndUserEmailIgnoreCase(normalizedNumber, currentUserEmail)
                || vehicleRepository.existsByVehicleNumber(normalizedNumber)) {
            throw new DuplicateVehicleNumberException(DUPLICATE_VEHICLE_NUMBER_MESSAGE);
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setUserEmail(currentUserEmail);
        vehicle.setVehicleNumber(normalizedNumber);
        vehicle.setCompany(request.company().trim());
        vehicle.setType(request.type().trim());
        vehicle.setBrand(request.brand().trim());
        vehicle.setModel(request.model().trim());
        vehicle.setStatus(request.status());

        // 2. Database-level persistence and unique constraint handling
        try {
            Vehicle saved = vehicleRepository.saveAndFlush(vehicle);
            return VehicleResponse.from(saved);
        } catch (DataIntegrityViolationException ex) {
            if (isVehicleNumberUniqueConstraintViolation(ex)) {
                throw new DuplicateVehicleNumberException(DUPLICATE_VEHICLE_NUMBER_MESSAGE, ex);
            }
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponse getVehicleById(Long id) {
        if (id == null) {
            throw new VehicleNotFoundException("Vehicle ID must not be null");
        }
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        Vehicle vehicle = vehicleRepository.findByIdAndUserEmailIgnoreCase(id, currentUserEmail)
                .or(() -> vehicleRepository.findById(id).filter(v -> v.getUserEmail() == null || v.getUserEmail().equalsIgnoreCase(currentUserEmail)))
                .orElseThrow(() -> new VehicleNotFoundException(id));
        return VehicleResponse.from(vehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponse> getAllVehicles() {
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        List<Vehicle> list = vehicleRepository.findAllByUserEmailIgnoreCase(currentUserEmail);
        if (list.isEmpty() && SecurityUtils.DEFAULT_USER_EMAIL.equalsIgnoreCase(currentUserEmail)) {
            List<Vehicle> all = vehicleRepository.findAll();
            if (!all.isEmpty()) {
                return all.stream()
                        .filter(v -> v.getUserEmail() == null || v.getUserEmail().equalsIgnoreCase(currentUserEmail))
                        .map(VehicleResponse::from)
                        .toList();
            }
        }
        return list.stream()
                .map(VehicleResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehicleResponse> getVehicles(Pageable pageable) {
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        Page<Vehicle> page = vehicleRepository.findAllByUserEmailIgnoreCase(currentUserEmail, pageable);
        if ((page == null || page.isEmpty()) && SecurityUtils.DEFAULT_USER_EMAIL.equalsIgnoreCase(currentUserEmail)) {
            Page<Vehicle> all = vehicleRepository.findAll(pageable);
            if (all != null && !all.isEmpty()) {
                return all.map(VehicleResponse::from);
            }
        }
        return page != null ? page.map(VehicleResponse::from) : Page.empty(pageable);
    }

    @Override
    @Transactional
    public VehicleResponse updateVehicle(Long id, UpdateVehicleRequest request) {
        if (id == null) {
            throw new VehicleNotFoundException("Vehicle ID must not be null");
        }

        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        Vehicle existing = vehicleRepository.findByIdAndUserEmailIgnoreCase(id, currentUserEmail)
                .or(() -> vehicleRepository.findById(id).filter(v -> v.getUserEmail() == null || v.getUserEmail().equalsIgnoreCase(currentUserEmail)))
                .orElseThrow(() -> new VehicleNotFoundException(id));

        validate(request);

        String normalizedNumber = VehicleNumberNormalizer.normalize(request.vehicleNumber());

        // 1. User-scoped duplicate check (excluding the current vehicle)
        if (vehicleRepository.existsByVehicleNumberAndUserEmailIgnoreCaseAndIdNot(normalizedNumber, currentUserEmail, id)
                || vehicleRepository.existsByVehicleNumberAndIdNot(normalizedNumber, id)) {
            throw new DuplicateVehicleNumberException(DUPLICATE_VEHICLE_NUMBER_MESSAGE);
        }

        // Update existing row while preserving original ID and owner
        existing.setVehicleNumber(normalizedNumber);
        existing.setCompany(request.company().trim());
        existing.setType(request.type().trim());
        existing.setBrand(request.brand().trim());
        existing.setModel(request.model().trim());
        existing.setStatus(request.status());

        // 2. Database-level persistence and unique constraint handling
        try {
            Vehicle saved = vehicleRepository.saveAndFlush(existing);
            return VehicleResponse.from(saved);
        } catch (DataIntegrityViolationException ex) {
            if (isVehicleNumberUniqueConstraintViolation(ex)) {
                throw new DuplicateVehicleNumberException(DUPLICATE_VEHICLE_NUMBER_MESSAGE, ex);
            }
            throw ex;
        }
    }

    @Override
    @Transactional
    public void deleteVehicle(Long id) {
        if (id == null) {
            throw new VehicleNotFoundException("Vehicle ID must not be null");
        }

        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        Vehicle vehicle = vehicleRepository.findByIdAndUserEmailIgnoreCase(id, currentUserEmail)
                .or(() -> vehicleRepository.findById(id).filter(v -> v.getUserEmail() == null || v.getUserEmail().equalsIgnoreCase(currentUserEmail)))
                .orElseThrow(() -> new VehicleNotFoundException(id));

        vehicleRepository.delete(vehicle);
    }

    private <T> void validate(T object) {
        Set<ConstraintViolation<T>> violations = validator.validate(object);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private boolean isVehicleNumberUniqueConstraintViolation(DataIntegrityViolationException ex) {
        Throwable cause = ex;
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null) {
                String lower = message.toLowerCase(Locale.ROOT);
                if (lower.contains("uk_vehicles_user_vehicle_number") ||
                    lower.contains("uk_vehicles_vehicle_number") ||
                    lower.contains("unique constraint") && lower.contains("vehicle_number")) {
                    return true;
                }
            }
            cause = cause.getCause();
        }
        return false;
    }
}
