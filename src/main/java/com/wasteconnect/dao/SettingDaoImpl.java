package com.wasteconnect.dao;

import com.wasteconnect.exception.DataAccessException;
import com.wasteconnect.model.SystemSetting;

import java.sql.*;
import java.util.*;

public class SettingDaoImpl extends BaseDao<SystemSetting, String> implements SettingDao {

    @Override
    public Optional<SystemSetting> findById(String key) {
        String sql = "SELECT * FROM settings WHERE \"key\" = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, key);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new SystemSetting(rs.getString("key"), rs.getString("value"), rs.getString("description")));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query setting: " + key, e);
        }
        return Optional.empty();
    }

    @Override
    public List<SystemSetting> findAll() {
        List<SystemSetting> list = new ArrayList<>();
        String sql = "SELECT * FROM settings ORDER BY \"key\" ASC;";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new SystemSetting(rs.getString("key"), rs.getString("value"), rs.getString("description")));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query settings", e);
        }
        return list;
    }

    @Override
    public String getValue(String key, String defaultValue) {
        return findById(key).map(SystemSetting::getValue).orElse(defaultValue);
    }

    @Override
    public double getDouble(String key, double defaultValue) {
        return findById(key).map(s -> s.getDoubleValue(defaultValue)).orElse(defaultValue);
    }

    @Override
    public int getInt(String key, int defaultValue) {
        return findById(key).map(s -> s.getIntValue(defaultValue)).orElse(defaultValue);
    }

    @Override
    public void setValue(String key, String value) {
        String sql = "MERGE INTO settings (\"key\", \"value\", description) KEY(\"key\") VALUES (?, ?, '');";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, key);
            pstmt.setString(2, value);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to persist setting: " + key, e);
        }
    }

    @Override
    public Map<String, String> getAllSettings() {
        Map<String, String> map = new LinkedHashMap<>();
        for (SystemSetting s : findAll()) {
            map.put(s.getKey(), s.getValue());
        }
        return map;
    }

    @Override
    public SystemSetting save(SystemSetting s) {
        setValue(s.getKey(), s.getValue());
        return s;
    }

    @Override
    public boolean update(SystemSetting s) {
        setValue(s.getKey(), s.getValue());
        return true;
    }

    @Override
    public boolean deleteById(String key) {
        String sql = "DELETE FROM settings WHERE \"key\" = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, key);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete setting: " + key, e);
        }
    }
}
