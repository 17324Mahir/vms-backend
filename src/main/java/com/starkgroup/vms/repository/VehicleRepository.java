package com.starkgroup.vms.repository;

import com.starkgroup.vms.domain.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    boolean existsByVehicleNumber(String vehicleNumber);

    boolean existsByVehicleNumberAndIdNot(String vehicleNumber, Long id);

    Optional<Vehicle> findByVehicleNumber(String vehicleNumber);

    List<Vehicle> findAllByUserEmailIgnoreCase(String userEmail);

    Page<Vehicle> findAllByUserEmailIgnoreCase(String userEmail, Pageable pageable);

    Optional<Vehicle> findByIdAndUserEmailIgnoreCase(Long id, String userEmail);

    boolean existsByVehicleNumberAndUserEmailIgnoreCase(String vehicleNumber, String userEmail);

    boolean existsByVehicleNumberAndUserEmailIgnoreCaseAndIdNot(String vehicleNumber, String userEmail, Long id);
}
