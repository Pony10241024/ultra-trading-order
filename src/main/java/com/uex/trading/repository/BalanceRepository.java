package com.uex.trading.repository;

import com.uex.trading.asset.Balance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BalanceRepository extends JpaRepository<Balance, Long> {
    Optional<Balance> findByUserIdAndAsset(String userId, String asset);
    List<Balance> findByUserId(String userId);
}
