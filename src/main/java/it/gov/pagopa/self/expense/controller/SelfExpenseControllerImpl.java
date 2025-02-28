package it.gov.pagopa.self.expense.controller;

import it.gov.pagopa.self.expense.dto.ChildResponseDTO;
import it.gov.pagopa.self.expense.dto.ExpenseDataDTO;
import it.gov.pagopa.self.expense.service.SelfExpenseService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
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
    public Mono<ResponseEntity<Void>> saveExpenseData(MultipartFile[] files, ExpenseDataDTO expenseData) {
        return selfExpenseService.saveExpenseData(files,expenseData)
                .then(Mono.just(ResponseEntity.ok().build()));
    }

    @Override
    public Mono<ResponseEntity<List<String>>> uploadFiles(@RequestParam("file") FilePart[] files) {
        try {
            List<String> fileNames = new ArrayList<>();
            Arrays.stream(files).forEach(file ->
                fileNames.add(file.filename())
            );
            return Mono.just(ResponseEntity.ok().body(fileNames));
        } catch (Exception e) {
            return Mono.just(ResponseEntity.internalServerError().build());
        }

    }

}
