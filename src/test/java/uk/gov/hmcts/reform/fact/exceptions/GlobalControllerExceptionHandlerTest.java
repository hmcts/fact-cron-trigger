package uk.gov.hmcts.reform.fact.exceptions;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalControllerExceptionHandlerTest {

    private GlobalControllerExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalControllerExceptionHandler();
    }

    @Test
    void shouldReturnInternalServerErrorWhenJsonConvertExceptionOccurs() throws JsonProcessingException {
        JsonConvertException exception = new JsonConvertException("Conversion failed");
        ResponseEntity<String> response = exceptionHandler.handleJsonConvertException(exception);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("{\"message\":\"Conversion failed\"}", response.getBody());
    }
}
