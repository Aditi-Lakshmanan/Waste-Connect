package com.wasteconnect.dao;

import com.wasteconnect.model.PickupRequest;
import com.wasteconnect.model.PickupStatus;

import java.util.List;

public interface PickupRequestDao extends Repository<PickupRequest, String> {
    List<PickupRequest> findByCitizen(String citizenId);
    List<PickupRequest> findPending();
    boolean updateStatus(String id, PickupStatus status, String collectorId);
}
