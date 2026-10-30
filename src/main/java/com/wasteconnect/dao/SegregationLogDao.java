package com.wasteconnect.dao;

import com.wasteconnect.model.SegregationLog;

import java.util.List;

public interface SegregationLogDao extends Repository<SegregationLog, String> {
    List<SegregationLog> findByCitizen(String citizenId);
    List<SegregationLog> findRecent(int limit);
}
