package it.gov.pagopa.self.expense.configuration;

import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.self.expense.constants.Constants;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class ExceptionMapTest {

    @Test
    void testThrowUnknownError() {

        ExceptionMap exceptionMap = new ExceptionMap();

        String unknownKey =   Constants.ExceptionCode.UNKNOWN_ERROR;
        String message =   Constants.ExceptionMessage.UNKNOWN_ERROR;


        RuntimeException exception = exceptionMap.throwException(unknownKey, message);

        assertTrue(exception instanceof ClientExceptionWithBody);

        ClientExceptionWithBody clientException = (ClientExceptionWithBody) exception;
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, clientException.getHttpStatus());
        assertEquals(Constants.ExceptionCode.UNKNOWN_ERROR, clientException.getCode());
        assertEquals(message, clientException.getMessage());
    }

    @Test
    void testThrowKnownError() {
        ExceptionMap exceptionMap = new ExceptionMap();

        String knownKey = Constants.ExceptionName.LOGIN_FAIL;
        String message = Constants.ExceptionMessage.LOGIN_FAIL;

        RuntimeException exception = exceptionMap.throwException(knownKey, message);

        assertTrue(exception instanceof ClientExceptionWithBody);

        ClientExceptionWithBody clientException = (ClientExceptionWithBody) exception;
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, clientException.getHttpStatus());
        assertEquals(Constants.ExceptionCode.LOGIN_FAIL, clientException.getCode());
        assertEquals(message, clientException.getMessage());
    }
}