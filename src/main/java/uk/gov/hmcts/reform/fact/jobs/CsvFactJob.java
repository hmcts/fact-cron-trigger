package uk.gov.hmcts.reform.fact.jobs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.fact.runner.CsvGenerator;

@Component
@Lazy
@Slf4j
public class CsvFactJob implements FactJob {

    private final CsvGenerator csvGenerator;

    public CsvFactJob(@Autowired CsvGenerator csvGenerator) {
        this.csvGenerator = csvGenerator;
    }

    @Override
    public void execute() {
        log.info("Running CSV generation job");
        csvGenerator.createCsvAndUpload();
        log.info("Finished CSV generation job");
    }
}
