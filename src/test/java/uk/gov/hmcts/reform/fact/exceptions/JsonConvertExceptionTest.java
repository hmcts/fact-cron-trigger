package uk.gov.hmcts.reform.fact.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonConvertExceptionTest {

    @Test
    void shouldCreateJsonConvertExceptionWithGivenMessage() {
        String expectedMessage = "Conversion failed";
        JsonConvertException exception = new JsonConvertException(expectedMessage);
        assertEquals(expectedMessage, exception.getMessage(), "Exception message should match");
    }
}
