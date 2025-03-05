package it.gov.pagopa.self.expense.model.mapper;

import it.gov.pagopa.self.expense.dto.ExpenseDataDTO;
import it.gov.pagopa.self.expense.model.ExpenseData;
import it.gov.pagopa.self.expense.utils.Utils;
import org.springframework.http.codec.multipart.FilePart;

import java.util.List;

public class ExpenseDataMapper {

    private ExpenseDataMapper(){}

    public static ExpenseData map(ExpenseDataDTO dto, List<FilePart> files){
        return ExpenseData.builder()
                .name(dto.getName())
                .surname(dto.getSurname())
                .amountCents(Utils.euroToCents(dto.getAmount()))
                .expenseDate(dto.getExpenseDate())
                .companyName(dto.getCompanyName())
                .entityId(dto.getEntityId())
                .userId(dto.getFiscalCode())
                .description(dto.getDescription())
                .filesName(files.stream().map(FilePart::filename).toList())
                .build();
    }
}
