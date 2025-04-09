package uk.gov.hmcts.reform.fact.services;

import com.azure.storage.blob.BlobServiceClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.reform.fact.factapi.FactClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest()
class FactApiIntegrationTest {

    @MockitoBean
    private AzureService azureService;
    @MockitoBean
    private BlobServiceClient blobServiceClient;
    @Autowired
    private FactClient factClient;
    @Autowired
    private FactService factService;

    @Test
    void compareFactAPIWithMethodToConvertToModel() throws JsonProcessingException {
        // Get the Data from Fact Api endpoint
        String rawApiCourtData = factClient.getAllCourtData();

        // Get the converted model from the Fact Service.
        JsonNode convertedModelList = factService.getCourtData();

        // Make sure they both equal one another in terms of content.
        // If not, then the conversion with the model classes has missed something or another variable has been
        // added in the response to the call to FACT API
        assertThat(
            StringUtils.difference(
                rawApiCourtData,
                new ObjectMapper().writeValueAsString(convertedModelList)
            )).isEmpty();
    }
}
