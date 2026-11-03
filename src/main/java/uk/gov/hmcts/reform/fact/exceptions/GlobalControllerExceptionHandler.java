package uk.gov.hmcts.reform.fact.exceptions;

import tools.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;

@ControllerAdvice
@Slf4j
public class GlobalControllerExceptionHandler {

    private static final String MESSAGE = "message";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";

    /**
     * Handles JSON conversion failures.
     *
     * @param ex the exception
     * @return the response entity
     */
    @ExceptionHandler(JsonConvertException.class)
    ResponseEntity<String> handleJsonConvertException(final JsonConvertException ex) {
        HashMap<String, String> error = new HashMap<>();
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set(CONTENT_TYPE, APPLICATION_JSON);
        error.put(MESSAGE, ex.getMessage());
        return new ResponseEntity<>(JsonMapper.builder().build().writeValueAsString(error),
                                    responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}
