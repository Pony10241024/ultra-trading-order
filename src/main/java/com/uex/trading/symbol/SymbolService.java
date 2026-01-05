package com.uex.trading.symbol;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class SymbolService {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Value("${exchange.binance.api-url}")
    private String binanceApiUrl;

    @Value("${exchange.okx.api-url}")
    private String okxApiUrl;

    @Value("${redis.keys.symbol-prefix}")
    private String symbolPrefix;

    public void syncSymbolsFromBinance() {
        try {
            log.info("Starting to sync symbols from Binance");
            WebClient webClient = webClientBuilder.baseUrl(binanceApiUrl).build();

            JsonNode response = webClient.get()
                    .uri("/api/v3/exchangeInfo")
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response != null && response.has("symbols")) {
                JsonNode symbols = response.get("symbols");
                List<SymbolInfo> symbolInfos = new ArrayList<>();

                for (JsonNode symbolNode : symbols) {
                    if ("TRADING".equals(symbolNode.get("status").asText())) {
                        SymbolInfo symbolInfo = parseBinanceSymbol(symbolNode);
                        symbolInfos.add(symbolInfo);
                    }
                }

                saveSymbolsToRedis(symbolInfos);
                log.info("Synced {} symbols from Binance", symbolInfos.size());
            }
        } catch (Exception e) {
            log.error("Failed to sync symbols from Binance", e);
        }
    }

    public void syncSymbolsFromOkx() {
        try {
            log.info("Starting to sync symbols from OKX");
            WebClient webClient = webClientBuilder.baseUrl(okxApiUrl).build();

            JsonNode response = webClient.get()
                    .uri("/api/v5/public/instruments?instType=SPOT")
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response != null && response.has("data")) {
                JsonNode data = response.get("data");
                List<SymbolInfo> symbolInfos = new ArrayList<>();

                for (JsonNode instNode : data) {
                    SymbolInfo symbolInfo = parseOkxSymbol(instNode);
                    symbolInfos.add(symbolInfo);
                }

                saveSymbolsToRedis(symbolInfos);
                log.info("Synced {} symbols from OKX", symbolInfos.size());
            }
        } catch (Exception e) {
            log.error("Failed to sync symbols from OKX", e);
        }
    }

    private SymbolInfo parseBinanceSymbol(JsonNode symbolNode) {
        SymbolInfo info = new SymbolInfo();
        info.setSymbol(symbolNode.get("symbol").asText());
        info.setBaseAsset(symbolNode.get("baseAsset").asText());
        info.setQuoteAsset(symbolNode.get("quoteAsset").asText());
        info.setExchange("BINANCE");
        info.setUpdateTime(System.currentTimeMillis());

        // 解析过滤器
        JsonNode filters = symbolNode.get("filters");
        for (JsonNode filter : filters) {
            String filterType = filter.get("filterType").asText();
            switch (filterType) {
                case "PRICE_FILTER":
                    info.setTickSize(new BigDecimal(filter.get("tickSize").asText()));
                    break;
                case "LOT_SIZE":
                    info.setMinOrderQty(new BigDecimal(filter.get("minQty").asText()));
                    info.setStepSize(new BigDecimal(filter.get("stepSize").asText()));
                    break;
                case "MIN_NOTIONAL":
                    info.setMinOrderAmount(new BigDecimal(filter.get("minNotional").asText()));
                    break;
            }
        }

        // 币安手续费默认值
        info.setMakerFee(new BigDecimal("0.001"));
        info.setTakerFee(new BigDecimal("0.001"));

        return info;
    }

    private SymbolInfo parseOkxSymbol(JsonNode instNode) {
        SymbolInfo info = new SymbolInfo();
        String instId = instNode.get("instId").asText();
        info.setSymbol(instId.replace("-", ""));
        info.setBaseAsset(instNode.get("baseCcy").asText());
        info.setQuoteAsset(instNode.get("quoteCcy").asText());
        info.setExchange("OKX");
        info.setUpdateTime(System.currentTimeMillis());

        info.setMinOrderQty(new BigDecimal(instNode.get("minSz").asText()));
        info.setTickSize(new BigDecimal(instNode.get("tickSz").asText()));
        info.setStepSize(new BigDecimal(instNode.get("lotSz").asText()));

        // OKX手续费默认值
        info.setMakerFee(new BigDecimal("0.0008"));
        info.setTakerFee(new BigDecimal("0.001"));

        // 计算最小下单金额
        BigDecimal minQty = info.getMinOrderQty();
        if (minQty != null) {
            info.setMinOrderAmount(minQty.multiply(new BigDecimal("10")));
        }

        return info;
    }

    private void saveSymbolsToRedis(List<SymbolInfo> symbolInfos) {
        RMap<String, SymbolInfo> symbolMap = redissonClient.getMap(symbolPrefix + "map");
        for (SymbolInfo symbolInfo : symbolInfos) {
            symbolMap.put(symbolInfo.getSymbol(), symbolInfo);
        }
    }

    public SymbolInfo getSymbolInfo(String symbol) {
        RMap<String, SymbolInfo> symbolMap = redissonClient.getMap(symbolPrefix + "map");
        return symbolMap.get(symbol);
    }

    public List<SymbolInfo> getAllSymbols() {
        RMap<String, SymbolInfo> symbolMap = redissonClient.getMap(symbolPrefix + "map");
        return new ArrayList<>(symbolMap.values());
    }
}
