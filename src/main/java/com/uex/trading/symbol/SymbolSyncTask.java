package com.uex.trading.symbol;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SymbolSyncTask implements CommandLineRunner {

    @Autowired
    private SymbolService symbolService;

    @Override
    public void run(String... args) {
        log.info("Initial symbol sync started");
        syncSymbols();
    }

    @Scheduled(fixedRateString = "${symbol.sync.fixed-rate}")
    public void scheduledSync() {
        log.info("Scheduled symbol sync started");
        syncSymbols();
    }

    private void syncSymbols() {
        try {
            symbolService.syncSymbolsFromBinance();
            symbolService.syncSymbolsFromOkx();
            log.info("Symbol sync completed");
        } catch (Exception e) {
            log.error("Symbol sync failed", e);
        }
    }
}
