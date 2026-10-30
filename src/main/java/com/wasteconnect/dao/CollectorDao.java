package com.wasteconnect.dao;

import com.wasteconnect.model.Collector;

import java.util.List;
import java.util.Optional;

public interface CollectorDao extends Repository<Collector, String> {
    Optional<Collector> findByEmployeeId(String employeeId);
    List<Collector> findByZone(String zoneId);
    boolean incrementPickups(String collectorId);
}
