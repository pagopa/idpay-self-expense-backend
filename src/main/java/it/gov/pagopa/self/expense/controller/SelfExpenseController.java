package it.gov.pagopa.self.expense.controller;

import it.gov.pagopa.self.expense.dto.ChildResponseDTO;
import it.gov.pagopa.self.expense.dto.ExpenseDataDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RequestMapping("/idpay/self-expense")
public interface SelfExpenseController {

    @GetMapping(value = "/get-child/{milAuthToken}")
    Mono<ResponseEntity<ChildResponseDTO>> getChildForUserId(@PathVariable("milAuthToken") String milAuthToken);

    @PostMapping(value = "/save-expense-data")
    Mono<ResponseEntity<Void>> saveExpenseData(
            @RequestPart("files") List<FilePart> files,
            @RequestPart("expenseData") ExpenseDataDTO expenseData);


}
