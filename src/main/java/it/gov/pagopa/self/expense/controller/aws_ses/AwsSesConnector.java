package it.gov.pagopa.self.expense.controller.aws_ses;

public interface AwsSesConnector {

    String sendEmail(String to, String subject, String body);

}
