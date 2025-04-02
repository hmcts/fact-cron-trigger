package uk.gov.hmcts.reform.fact.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.exceptions.JsonConvertException;
import uk.gov.hmcts.reform.fact.factapi.FactClient;

@Service
public class FactService {

    private final FactClient factClient;

    @Autowired
    public FactService(@Autowired FactClient factClient) {
        this.factClient = factClient;
    }

    public JsonNode getCourtData() {
        return callFactApiAndConvertToJson();
    }

    private JsonNode callFactApiAndConvertToJson() {
        try {
            // Explicit mapping to a model to make the CSV creation easier
            // Also increases performance by giving explicit indication of how the mapper will structure the JsonNode
            return new ObjectMapper().readTree(factClient.getAllCourtData());
        } catch (JsonProcessingException ex) {
            throw new JsonConvertException(String.format("Error converting JSON to court model: %s", ex.getMessage()));
        }
    }
}
