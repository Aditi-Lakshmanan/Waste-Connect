package com.wasteconnect.model;

import java.util.Objects;

/**
 * Key-value configuration entity governing dynamic engine parameters and weights.
 */
public class SystemSetting {
    private String key;
    private String value;
    private String description;

    public SystemSetting(String key, String value, String description) {
        this.key = Objects.requireNonNull(key);
        this.value = Objects.requireNonNull(value);
        this.description = description != null ? description : "";
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getDoubleValue(double defaultValue) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public int getIntValue(int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
