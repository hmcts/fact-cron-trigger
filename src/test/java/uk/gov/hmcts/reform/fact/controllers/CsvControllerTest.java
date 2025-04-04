package uk.gov.hmcts.reform.fact.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.reform.fact.services.FactService;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CsvControllerTest {

    @Mock
    private FactService factService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // Note, ignore warning here is SpringBoot does the try-with for us
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(new CsvController(factService)).build();
    }

    @Test
    void generateAndUploadCSV_shouldReturnJsonData() throws Exception {
        String jsonString = "{\"courtData\": [{\"name\": \"Aberdare County Court\"}]}";
        JsonNode mockJsonNode = objectMapper.readTree(jsonString);

        when(factService.getCourtData()).thenReturn(mockJsonNode);

        mockMvc.perform(get("/v1/generate-csv")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(mockJsonNode.toString()));
    }
}
