package uk.gov.hmcts.reform.fact.models;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Address {
    private List<String> addressLines;
    private String postcode;
    private String town;
    private String type;
    private String county;
    private String description;
    private String epimId;
    private String fieldsOfLaw;
}
