package it.gov.pagopa.self.expense.service;


import it.gov.pagopa.self.expense.model.OIDCProviderToken;

public interface OIDCService {

    boolean validateTokens(OIDCProviderToken oidcToken);

    String extractFiscalCodeFromIdToken(String idToken);

}
