package it.gov.pagopa.self.expense.service;

import it.gov.pagopa.self.expense.dto.ChildResponseDTO;
import reactor.core.publisher.Mono;

public interface SelfExpenseService {
    Mono<ChildResponseDTO> getChildForUserId(String userId, String initiativeId);
}
