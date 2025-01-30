package it.gov.pagopa.self.expense.dto;

import it.gov.pagopa.self.expense.model.Child;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
public class ChildResponseDTO {

    private List<Child> childList;
}
