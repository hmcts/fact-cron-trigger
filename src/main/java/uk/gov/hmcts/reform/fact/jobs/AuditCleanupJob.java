package uk.gov.hmcts.reform.fact.jobs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.fact.services.FactDataCleanupService;

@Component
@Slf4j
public class AuditCleanupJob implements FactJob {

    private final FactDataCleanupService factDataCleanupService;

    public AuditCleanupJob(@Autowired FactDataCleanupService factDataCleanupService) {
        this.factDataCleanupService = factDataCleanupService;
    }

    @Override
    public void execute() {
        log.info("Running audit cleanup job");
        factDataCleanupService.cleanupAudits();
        log.info("Finished audit cleanup job");
    }
}
