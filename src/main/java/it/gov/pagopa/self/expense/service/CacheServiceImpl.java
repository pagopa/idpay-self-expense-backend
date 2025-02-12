package it.gov.pagopa.self.expense.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
public class CacheServiceImpl implements CacheService {

    @Value("${app.cache.ttl}")
    private long ttlInSeconds;
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    @Autowired
    public CacheServiceImpl(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Boolean> saveToCache(String key, String value) {
        log.info("[CACHE-SERVICE][SAVE] Saving to cache with key: {}", key);
        return redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlInSeconds))
                .doOnSuccess(result -> log.info("[CACHE-SERVICE][SAVE]  Successfully saved to cache with key: {}", key))
                .doOnError(e -> log.error("[CACHE-SERVICE][SAVE]  Error saving to cache with key: {}", key, e));
    }

    @Override
    public Mono<String> getFromCache(String key) {
        log.info("[CACHE-SERVICE][GET] Retrieving from cache with key: {}", key);
        return redisTemplate.opsForValue().get(key)
                .flatMap(value -> {
                        log.info("[CACHE-SERVICE][GET] successfully retrieved from cache with key: {}", key);
                        return redisTemplate.opsForValue().delete(key)
                                .flatMap(result -> {
                                    if (Boolean.TRUE.equals(result)){
                                        log.info("[CACHE-SERVICE][GET] successfully removed key: {} from cache", key);
                                        return Mono.just(value);
                                    }else{
                                        log.info("[CACHE-SERVICE][GET] key: {} not removed", key);
                                        return  Mono.empty();
                                    }
                                });
                })
                .doOnError(e -> log.error("[CACHE-SERVICE][GET] Error retrieving from cache with key: {}", key, e));
    }
}