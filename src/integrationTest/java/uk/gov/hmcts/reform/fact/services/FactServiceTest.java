package uk.gov.hmcts.reform.fact.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.fact.factapi.FactClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest()
class FactServiceTest {

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
        // If not, then the converting has missed something or another variable has been
        // added to the API model in the meanwhile
        assertThat(
            StringUtils.difference(
                rawApiCourtData,
                new ObjectMapper().writeValueAsString(convertedModelList)
            )).isEmpty();
    }
}
