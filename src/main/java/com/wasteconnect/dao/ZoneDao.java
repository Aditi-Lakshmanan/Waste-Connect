package com.wasteconnect.dao;

import com.wasteconnect.model.Zone;

import java.util.Optional;

public interface ZoneDao extends Repository<Zone, String> {
    Optional<Zone> findByCode(String code);
}
