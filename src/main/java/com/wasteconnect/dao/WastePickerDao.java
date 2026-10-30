package com.wasteconnect.dao;

import com.wasteconnect.model.WastePicker;

import java.util.List;

public interface WastePickerDao extends Repository<WastePicker, String> {
    List<WastePicker> findPendingVerification();
    boolean setVerificationStatus(String pickerId, boolean verified);
    boolean addEarnings(String pickerId, double amount, double kg);
}
