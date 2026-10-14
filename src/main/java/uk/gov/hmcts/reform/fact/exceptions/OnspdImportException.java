package uk.gov.hmcts.reform.fact.exceptions;

public class OnspdImportException extends RuntimeException {

    public OnspdImportException(String message) {
        super(message);
    }

    public OnspdImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
