package it.gov.pagopa.self.expense.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        CacheServiceImpl.class,
})
@TestPropertySource(properties = {
        "app.cache.ttl=60"
})
class CacheServiceTest {

    private static final String TEST_KEY = "testKey";
    private static final String TEST_VALUE = "testValue";
    private static final Duration TTL_DURATION = Duration.ofSeconds(60); // Static TTL value

    @MockBean
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    @Value("${app.cache.ttl}")
    private long ttlInSeconds;

    @Autowired
    private CacheServiceImpl cacheService;

    @Test
    void testSaveToCache() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.set(TEST_KEY, TEST_VALUE, TTL_DURATION)).thenReturn(Mono.just(true));

        // When & Then
        StepVerifier.create(cacheService.saveToCache(TEST_KEY, TEST_VALUE))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void testGetFromCache() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(TEST_KEY)).thenReturn(Mono.just(TEST_VALUE));
        when(valueOperations.delete(TEST_KEY)).thenReturn(Mono.just(true));

        // When & Then
        StepVerifier.create(cacheService.getFromCache(TEST_KEY))
                .expectNext(TEST_VALUE)
                .verifyComplete();
    }

    @Test
    void testGetFromCache_deleteKo() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(TEST_KEY)).thenReturn(Mono.just(TEST_VALUE));
        when(valueOperations.delete(TEST_KEY)).thenReturn(Mono.just(false));

        // When & Then
        StepVerifier.create(cacheService.getFromCache(TEST_KEY))
                .verifyComplete();
    }

    @Test
    void testGetFromCache_NotFound() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(TEST_KEY)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(cacheService.getFromCache(TEST_KEY))
                .expectNextCount(0)
                .verifyComplete();
    }
}
