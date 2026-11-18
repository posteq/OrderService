package com.innowise.academy.orderservice.config;

import com.innowise.academy.orderservice.dto.response.UserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    public static final String USERS_CACHE = "users";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory,
                                          @Value("${app.cache.user-ttl}") Duration ttl){

        RedisCacheConfiguration orderConfiguration = RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .prefixCacheNameWith("order-service:")
                .serializeKeysWith(SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(SerializationPair
                        .fromSerializer(new JacksonJsonRedisSerializer<>(UserInfo.class)));

        return RedisCacheManager.builder(factory)
                .withCacheConfiguration(USERS_CACHE,orderConfiguration)
                .transactionAware()
                .build();

    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler();
    }
}
