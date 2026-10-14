package uk.gov.hmcts.reform.fact.models;

import java.time.YearMonth;

public record OnspdRelease(
    String itemId,
    String title,
    YearMonth releaseDate,
    String serviceUrl,
    long modified
) {

    public String sourceVersion() {
        return itemId + ":" + modified;
    }
}
