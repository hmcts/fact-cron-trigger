package uk.gov.hmcts.reform.fact.jobs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.fact.integrations.SlackMessageHelper;
import uk.gov.hmcts.reform.fact.integrations.SlackNotificationConstants;
import uk.gov.hmcts.reform.fact.services.FactDataService;

import java.util.Optional;

@Component
@Slf4j
public class UserCleanupJob implements FactJob {

    private final FactDataService factDataService;
    private final SlackMessageHelper slackMessageHelper;

    public UserCleanupJob(@Autowired FactDataService factDataService,
                          @Autowired SlackMessageHelper slackMessageHelper) {
        this.factDataService = factDataService;
        this.slackMessageHelper = slackMessageHelper;
    }

    @Override
    public boolean isApplicable(ScheduleTypes scheduleType) {
        return ScheduleTypes.USER_CLEANUP == scheduleType;
    }

    @Override
    public void execute() {
        log.info("Running user cleanup job");
        SlackNotificationConstants notification = SlackNotificationConstants.USER_CLEANUP;
        try {
            factDataService.cleanupUsers();
            slackMessageHelper.sendDailyCheckSummary(
                notification.getServiceName(),
                notification.getSuccessIcon(),
                Optional.empty()
            );
            log.info("Finished user cleanup job");
        } catch (RuntimeException ex) {
            slackMessageHelper.sendDailyCheckSummary(
                notification.getServiceName(),
                notification.getFailureIcon(),
                Optional.of(notification.getFailurePrefix() + SlackNotificationConstants.FAILURE_DETAILS_SUFFIX)
            );
            throw ex;
        }
    }
}
