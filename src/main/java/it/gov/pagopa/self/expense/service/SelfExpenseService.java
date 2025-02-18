package it.gov.pagopa.self.expense.service;

import it.gov.pagopa.self.expense.dto.ChildResponseDTO;
import it.gov.pagopa.self.expense.dto.ExpenseDataDTO;
import reactor.core.publisher.Mono;

public interface SelfExpenseService {
  
    Mono<ChildResponseDTO> getChildForUserId(String userId);

    Mono<Void> saveExpenseData(ExpenseDataDTO expenseData);
}
