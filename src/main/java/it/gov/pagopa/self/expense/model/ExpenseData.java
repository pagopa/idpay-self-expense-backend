package it.gov.pagopa.self.expense.model;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document("expense_data")
@Data
@Builder
@FieldNameConstants
public class ExpenseData {
    @Id
    private String id;
    private String name;
    private String surname;
    private Double amount;
    private LocalDateTime expenseDate;
    private String companyName;
    private String entityId;
    private String userId;
    private String description;
    private List<FileData> fileList;

}
