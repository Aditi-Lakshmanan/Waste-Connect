package com.wasteconnect.dao;

import com.wasteconnect.model.Household;

import java.util.List;

public interface HouseholdDao extends Repository<Household, String> {
    List<Household> findByZone(String zoneId);
    List<Household> findPage(int offset, int limit, String search);
    int count(String search);
}
