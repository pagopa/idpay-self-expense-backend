package it.gov.pagopa.self.expense.service.commands;

import reactor.core.publisher.Mono;

public interface DeleteInitiativeService {
    Mono<String> execute(String initiativeId);
}
