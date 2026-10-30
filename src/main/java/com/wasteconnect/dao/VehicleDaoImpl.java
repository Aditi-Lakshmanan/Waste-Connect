package com.wasteconnect.dao;

import com.wasteconnect.exception.DataAccessException;
import com.wasteconnect.model.LargeTruck;
import com.wasteconnect.model.SmallVehicle;
import com.wasteconnect.model.Vehicle;
import com.wasteconnect.model.VehicleType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VehicleDaoImpl extends BaseDao<Vehicle, String> implements VehicleDao {

    @Override
    public Optional<Vehicle> findById(String id) {
        String sql = "SELECT * FROM vehicles WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapVehicle(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query vehicle by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Vehicle> findByRegistration(String regNum) {
        String sql = "SELECT * FROM vehicles WHERE UPPER(registration_number) = UPPER(?);";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, regNum);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapVehicle(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query vehicle by registration: " + regNum, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Vehicle> findAll() {
        List<Vehicle> list = new ArrayList<>();
        String sql = "SELECT * FROM vehicles ORDER BY id ASC;";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapVehicle(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query vehicles", e);
        }
        return list;
    }

    @Override
    public List<Vehicle> findActive() {
        List<Vehicle> list = new ArrayList<>();
        String sql = "SELECT * FROM vehicles WHERE active = 1 ORDER BY id ASC;";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapVehicle(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query active vehicles", e);
        }
        return list;
    }

    @Override
    public Vehicle save(Vehicle vehicle) {
        String sql = "INSERT INTO vehicles (id, registration_number, vehicle_type, capacity_kg, active) VALUES (?, ?, ?, ?, ?);";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, vehicle.getId());
            pstmt.setString(2, vehicle.getRegistrationNumber());
            pstmt.setString(3, vehicle.getVehicleType().name());
            pstmt.setDouble(4, vehicle.getCapacityKg());
            pstmt.setInt(5, vehicle.isActive() ? 1 : 0);
            pstmt.executeUpdate();
            return vehicle;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save vehicle: " + vehicle.getRegistrationNumber(), e);
        }
    }

    @Override
    public boolean update(Vehicle vehicle) {
        String sql = "UPDATE vehicles SET registration_number = ?, vehicle_type = ?, capacity_kg = ?, active = ? WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, vehicle.getRegistrationNumber());
            pstmt.setString(2, vehicle.getVehicleType().name());
            pstmt.setDouble(3, vehicle.getCapacityKg());
            pstmt.setInt(4, vehicle.isActive() ? 1 : 0);
            pstmt.setString(5, vehicle.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update vehicle: " + vehicle.getId(), e);
        }
    }

    @Override
    public boolean deleteById(String id) {
        String sql = "DELETE FROM vehicles WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete vehicle: " + id, e);
        }
    }

    // POLYMORPHISM: Instantiates concrete Vehicle subtype
    private Vehicle mapVehicle(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String reg = rs.getString("registration_number");
        VehicleType type = VehicleType.valueOf(rs.getString("vehicle_type"));
        double capacity = rs.getDouble("capacity_kg");
        boolean active = rs.getInt("active") == 1;

        Vehicle v;
        if (type == VehicleType.LARGE_TRUCK) {
            v = new LargeTruck(id, reg, capacity);
        } else {
            v = new SmallVehicle(id, reg, capacity);
        }
        v.setActive(active);
        return v;
    }
}
