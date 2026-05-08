package uk.gov.hmcts.reform.fact.factdataapi;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "factDataApi", url = "${factdata.url}")
public interface FactDataClient {

    @PostMapping("${factdata.endpoint.create-and-upload-csv}")
    String createAndUploadCsv();
}
