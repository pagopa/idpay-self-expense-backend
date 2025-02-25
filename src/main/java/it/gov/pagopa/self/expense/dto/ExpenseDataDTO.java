package it.gov.pagopa.self.expense.dto;

import it.gov.pagopa.self.expense.model.FileData;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ExpenseDataDTO {

    private String name;
    private String surname;
    private BigDecimal amount;
    private LocalDateTime expenseDate;
    private String companyName;
    private String entityId;
    private String fiscalCode;
    private String description;
    private List<FileData> fileList;
}
