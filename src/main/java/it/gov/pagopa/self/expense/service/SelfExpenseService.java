package it.gov.pagopa.self.expense.service;

import it.gov.pagopa.self.expense.dto.ChildResponseDTO;
import it.gov.pagopa.self.expense.dto.ExpenseDataDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

import java.util.List;

public interface SelfExpenseService {
  
    Mono<ChildResponseDTO> getChildForUserId(String userId);

    Mono<Void> saveExpenseData(List<FilePart> files, ExpenseDataDTO expenseData);

    Mono<ResponseEntity<byte[]>> generateReportExcel(String initiativeId);
}
