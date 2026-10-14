package uk.gov.hmcts.reform.fact.jobs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.fact.integrations.SlackMessageHelper;
import uk.gov.hmcts.reform.fact.integrations.SlackNotificationConstants;
import uk.gov.hmcts.reform.fact.models.OnspdUpdateResult;
import uk.gov.hmcts.reform.fact.services.OnspdService;

import java.util.Optional;

@Component
@Slf4j
public class OnspdImportJob implements FactJob {

    private final OnspdService onspdService;
    private final SlackMessageHelper slackMessageHelper;

    public OnspdImportJob(OnspdService onspdService, SlackMessageHelper slackMessageHelper) {
        this.onspdService = onspdService;
        this.slackMessageHelper = slackMessageHelper;
    }

    @Override
    public boolean isApplicable(ScheduleTypes scheduleType) {
        return ScheduleTypes.ONSPD_IMPORT == scheduleType;
    }

    @Override
    public void execute() {
        log.info("Running ONSPD reference data import job");
        SlackNotificationConstants notification = SlackNotificationConstants.ONSPD_IMPORT;
        try {
            OnspdUpdateResult result = onspdService.updateIfRequired();
            if (result.updated()) {
                slackMessageHelper.sendLongMessage(String.format(
                    "*%s ONSPD reference data updated*%n"
                        + "> Release `%s`: %,d large-user or terminated postcodes uploaded to `%s`.",
                    notification.getSuccessIcon(),
                    result.release(),
                    result.recordCount(),
                    result.blobPath()
                ));
                log.info(
                    "Uploaded ONSPD release {} with {} records to {}",
                    result.release(),
                    result.recordCount(),
                    result.blobPath()
                );
            } else {
                log.info("No new ONSPD release found; latest release is {}", result.release());
            }
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
