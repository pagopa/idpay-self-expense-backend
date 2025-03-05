package it.gov.pagopa.self.expense.controller;

import it.gov.pagopa.self.expense.dto.ChildResponseDTO;
import it.gov.pagopa.self.expense.dto.ExpenseDataDTO;
import it.gov.pagopa.self.expense.service.SelfExpenseService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
public class SelfExpenseControllerImpl implements SelfExpenseController{

    private final SelfExpenseService selfExpenseService;
    public SelfExpenseControllerImpl(SelfExpenseService selfExpenseService) {
        this.selfExpenseService = selfExpenseService;
    }

    @Override
    public Mono<ResponseEntity<ChildResponseDTO>> getChildForUserId(String milAuthToken) {
        return selfExpenseService.getChildForUserId(milAuthToken)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Void>> saveExpenseData(List<FilePart> files, ExpenseDataDTO expenseData) {
        return selfExpenseService.saveExpenseData(files,expenseData)
                .then(Mono.just(ResponseEntity.ok().build()));
    }


}
