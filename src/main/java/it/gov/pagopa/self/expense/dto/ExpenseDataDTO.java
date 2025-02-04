package it.gov.pagopa.self.expense.dto;

import it.gov.pagopa.self.expense.model.FileData;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ExpenseDataDTO {

    private String name;
    private String surname;
    private Double amount;
    private LocalDateTime expenseDate;
    private String companyName;
    private String entityId;
    private String fiscalCode;
    private String initiativeId;
    private FileData file;
}
