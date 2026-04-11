package com.uex.trading.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.Map;

@Configuration
public class RedisConfig {

    static final String REDISSON_CONFIG_PREFIX = "spring.redis.redisson.config";

    private final Environment environment;

    public RedisConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        try {
            Config config = Config.fromYAML(resolveRedissonConfigYaml());
            config.setCodec(JsonJacksonCodec.INSTANCE);
            return Redisson.create(config);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Redisson client", e);
        }
    }

    String resolveRedissonConfigYaml() {
        String rawConfig = environment.getProperty(REDISSON_CONFIG_PREFIX);
        if (StringUtils.hasText(rawConfig)) {
            return rawConfig;
        }

        Map<String, Object> configMap = Binder.get(environment)
                .bind(REDISSON_CONFIG_PREFIX, Bindable.mapOf(String.class, Object.class))
                .orElseThrow(() -> new IllegalStateException(
                        "Missing Redisson config under '" + REDISSON_CONFIG_PREFIX + "'"));

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        return new Yaml(options).dump(configMap);
    }
}
