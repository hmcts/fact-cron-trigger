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

    @GetMapping("/generate-csv")
    public ResponseEntity<JsonNode> generateAndUploadCSV() {
        return ResponseEntity.ok(factService.getCourtData());
    }
}
