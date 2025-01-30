package it.gov.pagopa.self.expense.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


@Data
public class OIDCProviderToken {
    @JsonProperty("access_token")
    private String accessToken;
    @JsonProperty("token_type")
    private String tokenType;
    @JsonProperty("expires_in")
    private String expiresIn;
    @JsonProperty("id_token")
    private String idToken;
}
