package com.uex.trading.repository;

import com.uex.trading.asset.AssetFlow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetFlowRepository extends JpaRepository<AssetFlow, String> {
    List<AssetFlow> findByTradeAccount(String tradeAccount);
    List<AssetFlow> findByRelatedId(String relatedId);
}
