package com.wasteconnect.dao;

import com.wasteconnect.model.SystemSetting;

import java.util.Map;
import java.util.Optional;

public interface SettingDao extends Repository<SystemSetting, String> {
    String getValue(String key, String defaultValue);
    double getDouble(String key, double defaultValue);
    int getInt(String key, int defaultValue);
    void setValue(String key, String value);
    Map<String, String> getAllSettings();
}
