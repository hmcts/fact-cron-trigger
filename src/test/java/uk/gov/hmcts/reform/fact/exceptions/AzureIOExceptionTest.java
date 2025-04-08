package uk.gov.hmcts.reform.fact.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AzureIOExceptionTest {

    @Test
    void shouldCreateAzureIOExceptionWithGivenMessage() {
        String expectedMessage = "Azure blob storage failed";
        AzureIOException exception = new AzureIOException(expectedMessage);
        assertEquals(expectedMessage, exception.getMessage(), "Exception message should match");
    }
}
