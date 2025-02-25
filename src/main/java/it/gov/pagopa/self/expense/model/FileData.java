package it.gov.pagopa.self.expense.model;

import lombok.Data;

@Data
public class FileData {
    private String contentType;
    private String data;
    private String filename;
}
