package com.uex.trading.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {

    @Value("${spring.redis.redisson.config}")
    private String redissonConfig;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        try {
            Config config = Config.fromYAML(redissonConfig);
            return Redisson.create(config);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Redisson client", e);
        }
    }
}
