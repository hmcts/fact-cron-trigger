package uk.gov.hmcts.reform.fact.integrations;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SlackNotificationConstants {
    CSV("Old FACT CSV generation", ":bar_chart:", ":warning:", "CSV generation failed: "),
    USER_CLEANUP("FACT user cleanup", ":wastebasket:", ":warning:", "User cleanup failed: "),
    AUDIT_CLEANUP("FACT audit cleanup", ":wastebasket:", ":warning:", "Audit cleanup failed: ");

    private final String serviceName;
    private final String successIcon;
    private final String failureIcon;
    private final String failurePrefix;
}
