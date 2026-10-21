package uk.gov.hmcts.reform.fact.jobs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.fact.factapi.FactDataApiClient;
import uk.gov.hmcts.reform.fact.integrations.SlackMessageHelper;
import uk.gov.hmcts.reform.fact.integrations.SlackNotificationConstants;
import uk.gov.hmcts.reform.fact.models.PostcodeReferenceSynchronisationResult;

import java.util.Optional;

@Component
@Slf4j
public class OnspdImportJob implements FactJob {

    private final FactDataApiClient factDataApiClient;
    private final SlackMessageHelper slackMessageHelper;

    public OnspdImportJob(FactDataApiClient factDataApiClient, SlackMessageHelper slackMessageHelper) {
        this.factDataApiClient = factDataApiClient;
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
            PostcodeReferenceSynchronisationResult result = factDataApiClient.synchronisePostcodeReference();
            if (result.blobUpdated() || result.imported()) {
                slackMessageHelper.sendLongMessage(String.format(
                    "*%s ONSPD reference data synchronised*%n"
                        + "> Release `%s`: %,d large-user or terminated postcodes available from `%s`.",
                    notification.getSuccessIcon(),
                    result.release(),
                    result.recordCount(),
                    result.blobPath()
                ));
                log.info(
                    "Synchronised ONSPD release {} with {} records from {}",
                    result.release(),
                    result.recordCount(),
                    result.blobPath()
                );
            } else {
                log.info("ONSPD release {} is already synchronised", result.release());
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
