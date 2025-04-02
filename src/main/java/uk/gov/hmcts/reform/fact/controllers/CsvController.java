package uk.gov.hmcts.reform.fact.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.fact.factapi.FactClient;

@RestController
@RequestMapping(
    path = "/v1",
    produces = {MediaType.APPLICATION_JSON_VALUE}
)
public class CsvController {

    @Autowired
    private FactClient factClient;

    @GetMapping("/generate-csv")
    public ResponseEntity<String> generateAndUploadCSV() {
        return ResponseEntity.ok(factClient.getAllCourtData());
    }
}
