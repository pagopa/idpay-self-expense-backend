package it.gov.pagopa.self.expense.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document("expense_data")
@Data
@Builder
public class ExpenseData {

    private String name;
    private String surname;
    private Double amount;
    private LocalDateTime expenseDate;
    private String companyName;
    private String entityId;
    private String userId;
    private String initiativeId;
    private FileData file;

}
