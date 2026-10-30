package com.wasteconnect.dao;

import com.wasteconnect.model.Vehicle;

import java.util.List;
import java.util.Optional;

public interface VehicleDao extends Repository<Vehicle, String> {
    Optional<Vehicle> findByRegistration(String regNum);
    List<Vehicle> findActive();
}
