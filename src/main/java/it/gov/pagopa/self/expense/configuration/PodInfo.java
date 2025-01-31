package it.gov.pagopa.self.expense.configuration;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class PodInfo {

    @Value("${HOSTNAME:unknown}")
    private String podId;

}
