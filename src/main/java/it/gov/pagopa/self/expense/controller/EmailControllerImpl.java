package it.gov.pagopa.self.expense.controller;


import com.azure.communication.email.EmailAsyncClient;
import com.azure.communication.email.EmailClientBuilder;
import com.azure.communication.email.models.EmailAddress;
import com.azure.communication.email.models.EmailMessage;
import com.azure.core.util.polling.AsyncPollResponse;
import it.gov.pagopa.self.expense.dto.email.MessageDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class EmailControllerImpl implements  EmailController{

    @Value("${azure.communication.connection-string}")
    private String connectionString;



    @Override
    public Mono<ResponseEntity<String>> send(@RequestBody MessageDTO dto) {
        EmailAsyncClient client = new EmailClientBuilder()
                .connectionString(connectionString)
                .buildAsyncClient();

        EmailMessage message = new EmailMessage()
                .setSenderAddress("DoNotReply@9e2f6041-a3bd-4fac-8ee3-d38792eb0cec.azurecomm.net")
                .setToRecipients(new EmailAddress(dto.getEmailAddress()))
                .setSubject(dto.getSubject())
                .setBodyPlainText(dto.getPlainText())
                .setBodyHtml(dto.getHtml());

        return client.beginSend(message)
                .last()
                .flatMap(AsyncPollResponse::getFinalResult)
                .map(result -> ResponseEntity.ok("Email sent! ID: " + result.getId()))
                .onErrorResume(e -> Mono.just(ResponseEntity.internalServerError().body("Error: " + e.getMessage())));
    }

}
