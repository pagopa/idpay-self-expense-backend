package it.gov.pagopa.self.expense.dto;

import lombok.Builder;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

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
    private String description;
    private List<MultipartFile> fileList;
}
