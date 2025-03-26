package it.gov.pagopa.self.expense.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SelfDeclarationTextValues {

    private String type;
    private String description;
    private String value;
    private String code;

}
