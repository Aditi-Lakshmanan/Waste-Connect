package com.wasteconnect.dao;

import com.wasteconnect.model.User;

import java.util.Optional;

public interface UserDao extends Repository<User, String> {
    Optional<User> findByEmail(String email);
    boolean updatePassword(String id, String passwordHash);
}
