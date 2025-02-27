package it.gov.pagopa.self.expense.configuration;


import it.gov.pagopa.common.web.exception.ClientException;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.self.expense.constants.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Configuration
@Slf4j
public class ExceptionMap {

    private final Map<String, Function<String, ClientException>> exceptions = new HashMap<>();

    public ExceptionMap() {
        exceptions.put(Constants.ExceptionName.LOGIN_FAIL, message ->
                new ClientExceptionWithBody(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        Constants.ExceptionCode.LOGIN_FAIL,
                        message
                )
        );

        exceptions.put(Constants.ExceptionName.SESSION_FAIL, message ->
                new ClientExceptionWithBody(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        Constants.ExceptionCode.SESSION_FAIL,
                        message
                )
        );

        exceptions.put(Constants.ExceptionName.STATE_NOT_FOUND, message ->
                new ClientExceptionWithBody(
                        HttpStatus.NOT_FOUND,
                        Constants.ExceptionCode.STATE_NOT_FOUND,
                        message
                )
        );

        exceptions.put(Constants.ExceptionName.TOKEN_VALIDATION, message ->
                new ClientExceptionWithBody(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        Constants.ExceptionCode.TOKEN_VALIDATION,
                        message
                )
        );

        exceptions.put(Constants.ExceptionName.TOKEN_DESERIALIZATION, message ->
                new ClientExceptionWithBody(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        Constants.ExceptionCode.TOKEN_DESERIALIZATION,
                        message
                )
        );

        exceptions.put(Constants.ExceptionName.USER_SAVE_FAIL, message ->
                new ClientExceptionWithBody(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        Constants.ExceptionCode.USER_SAVE_FAIL,
                        message
                )
        );

        exceptions.put(Constants.ExceptionName.TOKEN_SAVE_FAIL, message ->
                new ClientExceptionWithBody(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        Constants.ExceptionCode.TOKEN_SAVE_FAIL,
                        message
                )
        );

        exceptions.put(Constants.ExceptionName.SESSION_NOT_FOUND, message ->
                new ClientExceptionWithBody(
                        HttpStatus.NOT_FOUND,
                        Constants.ExceptionCode.SESSION_NOT_FOUND,
                        message
                )
        );

        exceptions.put(Constants.ExceptionName.ANPR_INFO_NOT_FOUND, message ->
                new ClientExceptionWithBody(
                        HttpStatus.NOT_FOUND,
                        Constants.ExceptionCode.ANPR_INFO_NOT_FOUND,
                        message
                )
        );

        exceptions.put(Constants.ExceptionName.EXPENSE_DATA_ERROR_ON_SAVE_DB, message ->
                new ClientExceptionWithBody(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        Constants.ExceptionCode.EXPENSE_DATA_ERROR_ON_SAVE_DB,
                        message
                )
        );

        exceptions.put(Constants.ExceptionName.EXPENSE_DATA_FILE_SAVE, message ->
                new ClientExceptionWithBody(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        Constants.ExceptionCode.EXPENSE_DATA_ERROR_ON_SAVE_DB,
                        message
                )
        );

        exceptions.put(Constants.ExceptionName.EXPENSE_DATA_FILE_VALIDATION, message ->
                new ClientExceptionWithBody(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        Constants.ExceptionCode.EXPENSE_DATA_ERROR_ON_SAVE_DB,
                        message
                )
        );

    }

    public RuntimeException throwException(String exceptionKey, String message) {
        if (exceptions.containsKey(exceptionKey)) {
            return exceptions.get(exceptionKey).apply(message);
        }
        else {
            return new ClientExceptionWithBody(HttpStatus.INTERNAL_SERVER_ERROR,
                                            Constants.ExceptionCode.UNKNOWN_ERROR,
                                            Constants.ExceptionMessage.UNKNOWN_ERROR);
        }
    }

}

