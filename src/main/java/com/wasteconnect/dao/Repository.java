package com.wasteconnect.dao;

import java.util.List;
import java.util.Optional;

/**
 * ABSTRACTION & CO4: Generic base repository interface for data access objects.
 *
 * @param <T>  Entity type
 * @param <ID> Primary key identifier type
 */
public interface Repository<T, ID> {
    Optional<T> findById(ID id);
    List<T> findAll();
    T save(T entity);
    boolean update(T entity);
    boolean deleteById(ID id);
}
