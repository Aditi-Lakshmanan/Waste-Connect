package com.wasteconnect.dao;

import com.wasteconnect.model.RewardTransaction;

import java.util.List;

public interface RewardTransactionDao extends Repository<RewardTransaction, String> {
    List<RewardTransaction> findByCitizen(String citizenId);
}
