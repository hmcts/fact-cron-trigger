package uk.gov.hmcts.reform.fact.jobs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.fact.integrations.SlackMessageHelper;
import uk.gov.hmcts.reform.fact.integrations.SlackNotificationConstants;
import uk.gov.hmcts.reform.fact.services.FactDataCleanupService;

import java.util.Optional;

@Component
@Slf4j
public class AuditCleanupJob implements FactJob {

    private final FactDataCleanupService factDataCleanupService;
    private final SlackMessageHelper slackMessageHelper;

    public AuditCleanupJob(@Autowired FactDataCleanupService factDataCleanupService,
                           @Autowired SlackMessageHelper slackMessageHelper) {
        this.factDataCleanupService = factDataCleanupService;
        this.slackMessageHelper = slackMessageHelper;
    }

    @Override
    public boolean isApplicable(ScheduleTypes scheduleType) {
        return ScheduleTypes.AUDIT_CLEANUP == scheduleType;
    }

    @Override
    public void execute() {
        log.info("Running audit cleanup job");
        SlackNotificationConstants notification = SlackNotificationConstants.AUDIT_CLEANUP;
        try {
            factDataCleanupService.cleanupAudits();
            slackMessageHelper.sendDailyCheckSummary(
                notification.getServiceName(),
                notification.getSuccessIcon(),
                Optional.empty()
            );
            log.info("Finished audit cleanup job");
        } catch (RuntimeException ex) {
            slackMessageHelper.sendDailyCheckSummary(
                notification.getServiceName(),
                notification.getFailureIcon(),
                Optional.of(notification.getFailurePrefix() + ex.getMessage())
            );
            throw ex;
        }
    }
}
