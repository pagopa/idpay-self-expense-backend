package it.gov.pagopa.self.expense.controller;

import it.gov.pagopa.self.expense.dto.ChildResponseDTO;
import it.gov.pagopa.self.expense.dto.ExpenseDataDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;


@RequestMapping("/idpay/self-expense")
public interface SelfExpenseController {

    @GetMapping(value = "/get-child/{userId}/{initiativeId}")
    Mono<ResponseEntity<ChildResponseDTO>> getChildForUserId(@PathVariable("userId") String userId, @PathVariable("initiativeId") String initiativeId);

    @PostMapping(value = "/save-expense-data")
    Mono<ResponseEntity<Void>> saveExpenseData(@RequestBody ExpenseDataDTO expenseDataDTO);
}
