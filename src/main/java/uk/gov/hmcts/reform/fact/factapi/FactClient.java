package uk.gov.hmcts.reform.fact.factapi;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "factApi", url = "${fact.url}")
public interface FactClient {

    @GetMapping("${fact.endpoint.all-court-data}")
    String getAllCourtData();
}
