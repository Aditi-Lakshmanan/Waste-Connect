package com.wasteconnect.dao;

import com.wasteconnect.model.Complaint;
import com.wasteconnect.model.ComplaintEvent;
import com.wasteconnect.model.ComplaintStatus;

import java.util.List;

public interface ComplaintDao extends Repository<Complaint, String> {
    List<Complaint> findByCitizen(String citizenId);
    List<Complaint> findByCollector(String collectorId);
    List<Complaint> findByZone(String zoneId);
    List<Complaint> findUnresolved();
    List<Complaint> findSlaBreached();
    boolean updateStatus(String complaintId, ComplaintStatus status, String resolutionNotes);
    boolean assignCollector(String complaintId, String collectorId);
    List<ComplaintEvent> findEventsForComplaint(String complaintId);
    void logEvent(ComplaintEvent event);
}
