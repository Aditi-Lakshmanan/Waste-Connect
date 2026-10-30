package com.wasteconnect.dao;

import com.wasteconnect.exception.DataAccessException;
import com.wasteconnect.model.RewardTransaction;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RewardTransactionDaoImpl extends BaseDao<RewardTransaction, String> implements RewardTransactionDao {

    @Override
    public Optional<RewardTransaction> findById(String id) {
        String sql = "SELECT * FROM reward_transactions WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapTransaction(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query reward transaction: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<RewardTransaction> findAll() {
        List<RewardTransaction> list = new ArrayList<>();
        String sql = "SELECT * FROM reward_transactions ORDER BY timestamp DESC;";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapTransaction(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query all reward transactions", e);
        }
        return list;
    }

    @Override
    public List<RewardTransaction> findByCitizen(String citizenId) {
        List<RewardTransaction> list = new ArrayList<>();
        String sql = "SELECT * FROM reward_transactions WHERE citizen_id = ? ORDER BY timestamp DESC;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, citizenId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapTransaction(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query reward transactions for citizen: " + citizenId, e);
        }
        return list;
    }

    @Override
    public RewardTransaction save(RewardTransaction tx) {
        String sql = "INSERT INTO reward_transactions (id, citizen_id, type, points, description, timestamp) VALUES (?, ?, ?, ?, ?, ?);";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, tx.getId());
            pstmt.setString(2, tx.getCitizenId());
            pstmt.setString(3, tx.getType().name());
            pstmt.setInt(4, tx.getPoints());
            pstmt.setString(5, tx.getDescription());
            pstmt.setString(6, tx.getTimestamp().toString());
            pstmt.executeUpdate();
            return tx;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save reward transaction: " + tx.getId(), e);
        }
    }

    @Override
    public boolean update(RewardTransaction entity) {
        return false; // Transactions are append-only ledger entries
    }

    @Override
    public boolean deleteById(String id) {
        String sql = "DELETE FROM reward_transactions WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete reward transaction: " + id, e);
        }
    }

    private RewardTransaction mapTransaction(ResultSet rs) throws SQLException {
        return new RewardTransaction(
                rs.getString("id"),
                rs.getString("citizen_id"),
                RewardTransaction.Type.valueOf(rs.getString("type")),
                rs.getInt("points"),
                rs.getString("description"),
                LocalDateTime.parse(rs.getString("timestamp"))
        );
    }
}
