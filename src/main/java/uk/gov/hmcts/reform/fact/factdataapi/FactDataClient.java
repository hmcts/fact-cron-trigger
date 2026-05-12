package uk.gov.hmcts.reform.fact.factdataapi;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import uk.gov.hmcts.reform.fact.config.FeignOAuth2Config;

@FeignClient(name = "factDataApi", url = "${factdata.url}", configuration = FeignOAuth2Config.class)
public interface FactDataClient {

    @PostMapping("${factdata.endpoint.create-and-upload-csv}")
    String createAndUploadCsv();
}
