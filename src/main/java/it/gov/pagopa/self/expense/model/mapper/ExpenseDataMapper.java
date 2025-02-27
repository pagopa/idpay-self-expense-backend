package it.gov.pagopa.self.expense.model.mapper;

import it.gov.pagopa.self.expense.dto.ExpenseDataDTO;
import it.gov.pagopa.self.expense.model.ExpenseData;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class ExpenseDataMapper {

    private ExpenseDataMapper(){}

    public static ExpenseData map(ExpenseDataDTO dto, List<MultipartFile> files){
        return ExpenseData.builder()
                .name(dto.getName())
                .surname(dto.getSurname())
                .amount(dto.getAmount())
                .expenseDate(dto.getExpenseDate())
                .companyName(dto.getCompanyName())
                .entityId(dto.getEntityId())
                .userId(dto.getFiscalCode())
                .description(dto.getDescription())
                .filesName(files.stream().map(MultipartFile::getOriginalFilename).toList())
                .build();
    }
}
