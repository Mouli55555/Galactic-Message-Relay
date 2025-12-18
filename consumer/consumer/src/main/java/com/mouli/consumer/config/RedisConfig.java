package com.mouli.consumer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

@Configuration
public class RedisConfig {

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(
            @Value("${spring.data.redis.host}") String host,
            @Value("${spring.data.redis.port}") int port) {
        RedisStandaloneConfiguration cfg = new RedisStandaloneConfiguration(host, port);
        return new LettuceConnectionFactory(cfg);
    }
    @Bean
    public StringRedisTemplate redisTemplate(
            RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }
}
