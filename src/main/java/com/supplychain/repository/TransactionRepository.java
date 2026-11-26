package com.supplychain.repository;

import com.supplychain.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository
        extends JpaRepository<Transaction, Long> {

    List<Transaction> findByProductIdOrderByTimestampDesc(
        String productId
    );

    List<Transaction> findAllByOrderByTimestampDesc();

    boolean existsByTransactionHash(String transactionHash);
}