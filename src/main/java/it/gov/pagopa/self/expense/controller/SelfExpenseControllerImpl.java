package it.gov.pagopa.self.expense.controller;

import it.gov.pagopa.self.expense.configuration.ExceptionMap;
import it.gov.pagopa.self.expense.constants.Constants;
import it.gov.pagopa.self.expense.dto.ChildResponseDTO;
import it.gov.pagopa.self.expense.service.SelfExpenseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class SelfExpenseControllerImpl implements SelfExpenseController{

    private final SelfExpenseService selfExpenseService;
    private final ExceptionMap exceptionMap;

    public SelfExpenseControllerImpl(SelfExpenseService selfExpenseService, ExceptionMap exceptionMap ) {
        this.selfExpenseService = selfExpenseService;
        this.exceptionMap = exceptionMap;
    }

    @Override
    public Mono<ResponseEntity<ChildResponseDTO>> getChildForUserId(String userId, String initiativeId) {
        return selfExpenseService.getChildForUserId(userId, initiativeId)
                .switchIfEmpty(Mono.error(exceptionMap.throwException(
                    Constants.ExceptionName.ANPR_INFO_NOT_FOUND,
                    Constants.ExceptionMessage.ANPR_INFO_NOT_FOUND
                )))
                .map(ResponseEntity::ok);
    }
}
