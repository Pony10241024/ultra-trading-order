package com.uex.trading.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "persistenceExecutor")
    public Executor persistenceExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("persistence-");
        executor.setRejectedExecutionHandler((r, e) -> {
            // 队列满时记录警告，但不丢弃任务
            try {
                e.getQueue().put(r);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });
        executor.initialize();
        return executor;
    }
}
