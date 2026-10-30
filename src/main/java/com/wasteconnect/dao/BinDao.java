package com.wasteconnect.dao;

import com.wasteconnect.model.Bin;
import com.wasteconnect.model.BinStatus;

import java.util.List;
import java.util.Optional;

public interface BinDao extends Repository<Bin, String> {
    Optional<Bin> findByCode(String code);
    List<Bin> findByZone(String zoneId);
    List<Bin> findByStatus(BinStatus status);
    List<Bin> findRequiringPickup(String zoneId, double minPriorityOrFill);
    List<Bin> findPage(int offset, int limit, String zoneId, String wasteType, String search);
    int count(String zoneId, String wasteType, String search);
    boolean updateFillLevel(String binId, double fillPercentage, String status);
    boolean recordCollection(String binId);
    void updateBatchFillLevels(List<Bin> bins);
}
