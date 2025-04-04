package uk.gov.hmcts.reform.fact.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.fact.services.FactService;

@RestController
@RequestMapping(
    path = "/v1",
    produces = {MediaType.APPLICATION_JSON_VALUE}
)
public class CsvController {

    private final FactService factService;

    public CsvController(@Autowired FactService factService) {
        this.factService = factService;
    }

    /**
     * Used primarilly for local running for further changes
     * For the main purpose of not having to restart the application over and over.
     * Also with functional tests in the pipeline we can check the processes work
     * end-to-end as expected
     * @return A response entity that includes the CSV for download when accessing the endpoint
     */
    @GetMapping("/generate-csv")
    public ResponseEntity<JsonNode> generateAndUploadCSV() {
        return ResponseEntity.ok(factService.getCourtData());
    }
}
