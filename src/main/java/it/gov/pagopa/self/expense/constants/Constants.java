package it.gov.pagopa.self.expense.constants;

import java.util.List;

public class Constants {

    public static final List<String> CONTENT_TYPES = List.of("application/pdf","image/jpeg", "image/png");
    public static final String FILE_PATH_TEMPLATE = "%s/%s";

    public static final class ExceptionCode {
        public static final String LOGIN_FAIL = "LOGIN_FAIL";
        public static final String SESSION_FAIL = "SESSION_FAIL";
        public static final String SESSION_NOT_FOUND = "SESSION_NOT_FOUND";
        public static final String STATE_NOT_FOUND = "STATE_NOT_FOUND";
        public static final String TOKEN_VALIDATION = "TOKEN_VALIDATION";
        public static final String TOKEN_DESERIALIZATION = "TOKEN_DESERIALIZATION";
        public static final String USER_SAVE_FAIL = "USER_SAVE_FAIL";
        public static final String TOKEN_SAVE_FAIL = "TOKEN_SAVE_FAIL";
        public static final String UNKNOWN_ERROR = "UNKNOWN_ERROR" ;
        public static final String ANPR_INFO_NOT_FOUND = "ANPR_INFO_NOT_FOUND";
        public static final String EXPENSE_DATA_ERROR_ON_SAVE_DB = "EXPENSE_DATA_ERROR_DB_SAVE";
        public static final String EXPENSE_DATA_FILE_SAVE = "EXPENSE_DATA_FILE_SAVE";
        public static final String EXPENSE_DATA_FILE_VALIDATION = "EXPENSE_DATA_FILE_VALIDATION";
        public static final String EXPENSE_DATA_FISCAL_CODE_NOT_FOUND = "EXPENSE_DATA_FISCAL_CODE_NOT_FOUND";
        private ExceptionCode() {}
    }

    public static final class ExceptionMessage {
        public static final String LOGIN_FAIL = "Login operation failed due to an unknown error.";
        public static final String SESSION_FAIL = "Session operation failed due to an unknown error.";
        public static final String SESSION_NOT_FOUND = "The session could not be found.";
        public static final String STATE_NOT_FOUND = "State not found in the cache.";
        public static final String TOKEN_VALIDATION = "Token validation failed.";
        public static final String TOKEN_DESERIALIZATION = "Error occurred during token deserialization.";
        public static final String USER_SAVE_FAIL = "User data could not be saved.";
        public static final String TOKEN_SAVE_FAIL = "Token could not be saved to the cache.";
        public static final String UNKNOWN_ERROR = "Operation failed for unknown reason";
        public static final String ANPR_INFO_NOT_FOUND = "Anpr info could not be found.";
        public static final String EXPENSE_DATA_ERROR_ON_SAVE_DB = "Error on save into DB expense_data document";
        public static final String EXPENSE_DATA_FILE_SAVE = "Error on save file into blob azure";
        public static final String EXPENSE_DATA_FILE_VALIDATION = "File validation fail";
        public static final String EXPENSE_DATA_FISCAL_CODE_NOT_FOUND = "Fiscal code not found";

        private ExceptionMessage() {}
    }

    public static final class ExceptionName {
        public static final String LOGIN_FAIL = "Login Failure";
        public static final String SESSION_FAIL = "Session Failure";
        public static final String SESSION_NOT_FOUND = "Session Not Found";
        public static final String STATE_NOT_FOUND = "State Not Found";
        public static final String TOKEN_VALIDATION = "Token Validation Failure";
        public static final String TOKEN_DESERIALIZATION = "Token Deserialization Failure";
        public static final String USER_SAVE_FAIL = "User Save Failure";
        public static final String TOKEN_SAVE_FAIL = "Token Save Failure";
        public static final String ANPR_INFO_NOT_FOUND = "Anpr info Not Found";
        public static final String EXPENSE_DATA_ERROR_ON_SAVE_DB = "Error on save into DB expense_data document";
        public static final String EXPENSE_DATA_FILE_SAVE = "Error on save file into blob azure";
        public static final String EXPENSE_DATA_FILE_VALIDATION = "File validation fail";
        public static final String EXPENSE_DATA_FISCAL_CODE_NOT_FOUND = "Fiscal code not found";

        private ExceptionName() {}
    }

    private Constants() {}

    public static final String UUID_REGEX = "^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}-\\w+-\\d+$";
}
