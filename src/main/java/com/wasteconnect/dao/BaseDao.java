package com.wasteconnect.dao;

import com.wasteconnect.exception.DataAccessException;
import com.wasteconnect.util.AppLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * CO4 & ABSTRACTION: Generic abstract Data Access Object foundation providing
 * managed connection access, transaction wrappers, and standardized mapping helpers.
 *
 * @param <T>  Entity type
 * @param <ID> Key type
 */
public abstract class BaseDao<T, ID> implements Repository<T, ID> {
    protected final Logger logger = AppLogger.getLogger(getClass());

    protected Connection getConnection() {
        return DatabaseConnection.getInstance().getConnection();
    }

    /**
     * Functional interface for mapping a single SQL ResultSet row to an entity.
     */
    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    protected void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException e) {
                logger.warning("Failed to rollback SQL transaction: " + e.getMessage());
            }
        }
    }
}
