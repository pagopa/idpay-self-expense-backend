package it.gov.pagopa.self.expense.controller;

import it.gov.pagopa.self.expense.dto.ChildResponseDTO;
import it.gov.pagopa.self.expense.dto.ExpenseDataDTO;
import it.gov.pagopa.self.expense.service.SelfExpenseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class SelfExpenseControllerImpl implements SelfExpenseController{

    private final SelfExpenseService selfExpenseService;
    public SelfExpenseControllerImpl(SelfExpenseService selfExpenseService) {
        this.selfExpenseService = selfExpenseService;
    }

    @Override
    public Mono<ResponseEntity<ChildResponseDTO>> getChildForUserId(String userId, String initiativeId) {
       //Recuperare tramite il milAuthToken (usato come Key su redis) il fiscalCode del richiedente (Papà, Mamma o Tutore del minore)
        //Una volta recuperato il fiscalCode sarà necessario, tramite pdv, convertirlo in uuid
        // Dopodiché continuare il flusso
        return selfExpenseService.getChildForUserId(userId, initiativeId)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Void>> saveExpenseData(ExpenseDataDTO expenseData) {
        return selfExpenseService.saveExpenseData(expenseData)
                .then(Mono.just(ResponseEntity.ok().build()));
    }
}
