package uk.gov.hmcts.reform.fact.integrations;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.reform.fact.config.OnspdProperties;
import uk.gov.hmcts.reform.fact.exceptions.OnspdImportException;
import uk.gov.hmcts.reform.fact.models.OnspdRelease;

import java.net.URI;
import java.time.Duration;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class OnspdClient {

    static final String CATALOGUE_QUERY =
        "owner:ONSGeography_data AND tags:PRD_ONSPD AND type:\"Feature Service\"";
    // ArcGIS evaluates this SQL-like Feature Service expression remotely before returning JSON records.
    static final String POSTCODE_SELECTION_FILTER = "usrtypind = 1 OR doterm IS NOT NULL";
    static final String POSTCODE_FIELDS = "ObjectId,pcds,dointr,doterm,usrtypind,gridind,lat,long";

    private static final Pattern RELEASE_PATTERN = Pattern.compile(
        "\\((January|February|March|April|May|June|July|August|September|October|November|December) "
            + "(\\d{4})\\)",
        Pattern.CASE_INSENSITIVE
    );
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH);

    private final RestTemplate restTemplate;
    private final OnspdProperties properties;

    @Autowired
    public OnspdClient(RestTemplateBuilder restTemplateBuilder, OnspdProperties properties) {
        this(
            restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(30))
                .readTimeout(Duration.ofMinutes(2))
                .build(),
            properties
        );
    }

    OnspdClient(RestTemplate restTemplate, OnspdProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    /**
     * Finds the most recent valid ONSPD hosted-table release in the ArcGIS catalogue.
     * The release date is parsed from each title because catalogue creation order alone is not authoritative.
     *
     * @return latest valid ONSPD release
     * @throws OnspdImportException if the catalogue cannot be read or contains no valid release
     */
    public OnspdRelease findLatestRelease() {
        URI uri = UriComponentsBuilder.fromUriString(properties.getCatalogueUrl())
            .queryParam("f", "json")
            .queryParam("num", 100)
            .queryParam("sortField", "created")
            .queryParam("sortOrder", "desc")
            .queryParam("q", CATALOGUE_QUERY)
            .build()
            .encode()
            .toUri();

        JsonNode response = get(uri, "catalogue search");

        List<OnspdRelease> releases = new ArrayList<>();
        response.path("results").forEach(result -> toRelease(result).ifPresent(releases::add));

        return releases.stream()
            .max(Comparator.comparing(OnspdRelease::releaseDate).thenComparingLong(OnspdRelease::modified))
            .orElseThrow(() -> new OnspdImportException("No ONSPD hosted-table release found in the catalogue"));
    }

    /**
     * Counts the large-user and terminated postcodes selected for the reduced reference file.
     * The count is used to detect incomplete or changing exports before a file is uploaded.
     *
     * @param release ONSPD release to query
     * @return number of matching postcode records
     * @throws OnspdImportException if ArcGIS cannot return a valid count
     */
    public long getFilteredPostcodeCount(OnspdRelease release) {
        URI uri = filteredPostcodeQueryUri(release)
            .queryParam("returnCountOnly", true)
            .build()
            .encode()
            .toUri();

        JsonNode response = get(uri, "record count query");
        if (!response.path("count").canConvertToLong()) {
            throw new OnspdImportException("ONSPD record count response did not contain a valid count");
        }
        return response.path("count").asLong();
    }

    /**
     * Retrieves the next page of selected postcode attributes after the supplied object ID.
     * Paging by object ID keeps query performance stable across the complete dataset.
     *
     * @param release ONSPD release to query
     * @param lastObjectId final object ID processed from the preceding page
     * @return postcode attributes ordered by object ID
     * @throws OnspdImportException if ArcGIS cannot return the requested page
     */
    public List<JsonNode> getPostcodeRecordsAfter(OnspdRelease release, long lastObjectId) {
        URI uri = filteredPostcodeQueryUri(release)
            .queryParam("outFields", POSTCODE_FIELDS)
            .queryParam("returnGeometry", false)
            .queryParam("orderByFields", "ObjectId")
            .queryParam("resultRecordCount", properties.getPageSize())
            .replaceQueryParam("where", postcodePageFilter(lastObjectId))
            .build()
            .encode()
            .toUri();

        JsonNode response = get(uri, "postcode query after ObjectId " + lastObjectId);

        List<JsonNode> records = new ArrayList<>();
        response.path("features").forEach(feature -> records.add(feature.path("attributes")));
        return records;
    }

    /**
     * Creates a query URI restricted to records required by the postcode reference file.
     * Filtering within ArcGIS avoids transferring roughly the entire ONSPD dataset to the job.
     *
     * @param release ONSPD release to query
     * @return URI builder containing the common ArcGIS query parameters
     * @throws OnspdImportException if the release URL is not an approved HTTPS endpoint
     */
    private UriComponentsBuilder filteredPostcodeQueryUri(OnspdRelease release) {
        validateServiceUrl(release.serviceUrl());
        return UriComponentsBuilder.fromUriString(release.serviceUrl() + "/0/query")
            .queryParam("f", "json")
            .queryParam("where", POSTCODE_SELECTION_FILTER);
    }

    /**
     * Builds the ArcGIS filter for the next postcode page using keyset pagination.
     * This avoids the performance degradation caused by increasingly high result offsets.
     *
     * @param lastObjectId final object ID processed from the preceding page
     * @return ArcGIS where expression for the next page
     */
    private String postcodePageFilter(long lastObjectId) {
        return "(" + POSTCODE_SELECTION_FILTER + ") AND ObjectId > " + lastObjectId;
    }

    /**
     * Converts a catalogue result into a release only when its title and required fields are valid.
     * Ignoring malformed results prevents an unrelated or incomplete catalogue item from being imported.
     *
     * @param result ArcGIS catalogue result
     * @return parsed release, or empty when the result is not a valid ONSPD hosted table
     */
    private Optional<OnspdRelease> toRelease(JsonNode result) {
        String title = result.path("title").asText();
        if (!title.startsWith("ONS Postcode Directory") || !title.contains("Hosted Table")) {
            return Optional.empty();
        }

        Matcher matcher = RELEASE_PATTERN.matcher(title);
        if (!matcher.find()) {
            return Optional.empty();
        }

        try {
            Month month = Month.from(MONTH_FORMATTER.parse(capitalise(matcher.group(1))));
            YearMonth releaseDate = YearMonth.of(Integer.parseInt(matcher.group(2)), month);
            if (!result.path("id").isTextual()
                || !result.path("url").isTextual()
                || !result.path("modified").canConvertToLong()) {
                return Optional.empty();
            }
            return Optional.of(new OnspdRelease(
                result.path("id").asText(),
                title,
                releaseDate,
                result.path("url").asText(),
                result.path("modified").asLong()
            ));
        } catch (DateTimeParseException | NumberFormatException ex) {
            return Optional.empty();
        }
    }

    /**
     * Performs an ArcGIS GET request with bounded retries and validates its JSON response.
     * Retrying transient failures prevents a single remote error from aborting the daily import.
     *
     * @param uri ArcGIS endpoint to call
     * @param operation description included in logs and exceptions
     * @return validated response body
     * @throws OnspdImportException when all attempts fail or ArcGIS returns an error response
     */
    private JsonNode get(URI uri, String operation) {
        OnspdImportException lastFailure = null;
        int maxAttempts = Math.max(1, properties.getMaxAttempts());
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                JsonNode response = restTemplate.getForObject(uri, JsonNode.class);
                if (response == null) {
                    throw new OnspdImportException("Empty response received from ONSPD source");
                }
                validateArcGisResponse(response, operation);
                return response;
            } catch (RestClientException ex) {
                lastFailure = new OnspdImportException("Unable to retrieve ONSPD data during " + operation, ex);
            } catch (OnspdImportException ex) {
                lastFailure = ex;
            }

            if (attempt < maxAttempts) {
                log.warn(
                    "ONSPD request failed during {} (attempt {} of {}): {}",
                    operation,
                    attempt,
                    maxAttempts,
                    lastFailure.getMessage()
                );
                waitBeforeRetry(attempt, operation);
            }
        }
        throw lastFailure;
    }

    /**
     * Applies exponential backoff between failed ArcGIS requests.
     * Preserving the interrupted state allows application shutdown to proceed correctly.
     *
     * @param attempt number of the attempt that failed
     * @param operation description included if the wait is interrupted
     * @throws OnspdImportException if the retry wait is interrupted
     */
    private void waitBeforeRetry(int attempt, String operation) {
        long delay = Math.max(0, properties.getInitialRetryBackoffMillis())
            * (1L << Math.min(attempt - 1, 10));
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new OnspdImportException("Interrupted while retrying " + operation, ex);
        }
    }

    /**
     * Ensures a catalogue-provided service URL uses HTTPS and the configured trusted host.
     * This prevents catalogue data from directing the import job to an arbitrary endpoint.
     *
     * @param serviceUrl service URL supplied by the ArcGIS catalogue
     * @throws OnspdImportException if the URL is invalid or is not an approved endpoint
     */
    private void validateServiceUrl(String serviceUrl) {
        URI uri;
        try {
            uri = URI.create(serviceUrl);
        } catch (IllegalArgumentException ex) {
            throw new OnspdImportException("ONSPD catalogue returned an invalid service URL", ex);
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())
            || !properties.getAllowedServiceHost().equalsIgnoreCase(uri.getHost())) {
            throw new OnspdImportException("ONSPD catalogue returned a service URL from an unexpected host");
        }
    }

    /**
     * Converts an ArcGIS error object into an import exception containing its available details.
     * ArcGIS can return HTTP success with an error in the JSON body, so status handling alone is insufficient.
     *
     * @param response ArcGIS JSON response
     * @param operation description of the request being validated
     * @throws OnspdImportException if the response contains an ArcGIS error object
     */
    private void validateArcGisResponse(JsonNode response, String operation) {
        if (response.has("error")) {
            JsonNode error = response.path("error");
            List<String> detailValues = new ArrayList<>();
            error.path("details").forEach(detail -> detailValues.add(detail.asText()));
            String description = "code " + error.path("code").asText("unknown")
                + ": " + error.path("message").asText("No message supplied");
            if (!detailValues.isEmpty()) {
                description += " (" + String.join("; ", detailValues) + ")";
            }
            throw new OnspdImportException("ArcGIS returned an error during " + operation + " - " + description);
        }
    }

    private String capitalise(String value) {
        return value.substring(0, 1).toUpperCase(Locale.ENGLISH)
            + value.substring(1).toLowerCase(Locale.ENGLISH);
    }
}
