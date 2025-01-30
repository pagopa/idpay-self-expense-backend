package it.gov.pagopa.self.expense.service;

import reactor.core.publisher.Mono;


public interface CacheService {

    Mono<Boolean> saveToCache(String key, String value);

    Mono<String> getFromCache(String key);
}