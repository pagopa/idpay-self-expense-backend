package it.gov.pagopa.self.expense.dto.email;

import lombok.Data;

@Data
public class MessageDTO
{
    private String senderAddress;
    private String emailAddress;
    private String subject;
    private String plainText;
    private String html;
}
