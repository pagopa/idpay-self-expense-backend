package it.gov.pagopa.self.expense.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MilAuthAccessToken {
    @JsonProperty("access_token")
    private String accessToken;
    @JsonProperty("token_type")
    private String tokenType;
    @JsonProperty("expires_in")
    private String expiresIn;
}
