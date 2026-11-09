package com.starkgroup.vms.controller;

import com.starkgroup.vms.dto.CreateVehicleRequest;
import com.starkgroup.vms.dto.PageResponse;
import com.starkgroup.vms.dto.UpdateVehicleRequest;
import com.starkgroup.vms.dto.VehicleResponse;
import com.starkgroup.vms.service.VehicleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping({"/vehicles", "/api/vehicles"})
@Validated
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping
    public ResponseEntity<VehicleResponse> createVehicle(@Valid @RequestBody CreateVehicleRequest request) {
        VehicleResponse created = vehicleService.createVehicle(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public ResponseEntity<?> getVehicles(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size,
            @RequestParam(name = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "asc") String sortDir,
            HttpServletRequest request
    ) {
        String uri = request != null ? request.getRequestURI() : "";
        boolean isVehiclesDirect = uri != null && (uri.equals("/vehicles") || uri.startsWith("/vehicles/"));

        // If explicitly targeting /vehicles, OR client supplies pagination query parameters:
        if (isVehiclesDirect || page != null || size != null) {
            int pageNum = page != null ? Math.max(page, 0) : 0;
            int pageSize = size != null ? Math.max(size, 1) : 10;
            Sort sort = "desc".equalsIgnoreCase(sortDir)
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(pageNum, pageSize, sort);
            Page<VehicleResponse> vehiclesPage = vehicleService.getVehicles(pageable);
            return ResponseEntity.ok(PageResponse.from(vehiclesPage));
        }

        // Backward compatibility: Deployed frontend calling GET /api/vehicles without page params
        List<VehicleResponse> vehicles = vehicleService.getAllVehicles();
        return ResponseEntity.ok(vehicles);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponse> getVehicleById(
            @PathVariable("id") @Positive(message = "Vehicle ID must be a positive number") Long id) {
        VehicleResponse vehicle = vehicleService.getVehicleById(id);
        return ResponseEntity.ok(vehicle);
    }

    @PutMapping("/{id}")
    public ResponseEntity<VehicleResponse> updateVehicle(
            @PathVariable("id") @Positive(message = "Vehicle ID must be a positive number") Long id,
            @Valid @RequestBody UpdateVehicleRequest request) {
        VehicleResponse updated = vehicleService.updateVehicle(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehicle(
            @PathVariable("id") @Positive(message = "Vehicle ID must be a positive number") Long id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }
}
