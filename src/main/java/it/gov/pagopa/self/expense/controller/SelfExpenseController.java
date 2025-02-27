package it.gov.pagopa.self.expense.controller;

import it.gov.pagopa.self.expense.dto.ChildResponseDTO;
import it.gov.pagopa.self.expense.dto.ExpenseDataDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.util.List;


@RequestMapping("/idpay/self-expense")
public interface SelfExpenseController {

    @GetMapping(value = "/get-child/{milAuthToken}")
    Mono<ResponseEntity<ChildResponseDTO>> getChildForUserId(@PathVariable("milAuthToken") String milAuthToken);

    @PostMapping(value = "/save-expense-data")
    Mono<ResponseEntity<Void>> saveExpenseData(
            @RequestParam("files") List<MultipartFile> files,
            @RequestPart("expenseData") ExpenseDataDTO expenseData);
}
