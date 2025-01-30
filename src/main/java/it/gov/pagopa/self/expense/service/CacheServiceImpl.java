package it.gov.pagopa.self.expense.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class CacheServiceImpl implements  CacheService{
    @Value("${app.cache.ttl}")
    private long ttlInSeconds;
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    @Autowired
    public CacheServiceImpl(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Boolean> saveToCache(String key, String value) {
        return redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlInSeconds));
    }

    @Override
    public Mono<String> getFromCache(String key) {
        return redisTemplate.opsForValue().getAndDelete(key);
    }
}