package uk.gov.hmcts.reform.fact.models;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Court {
    private String name;
    private Double lat;
    private Double lon;
    private Integer number;
    private Integer cciCode;
    private Integer magistrateCode;
    private String slug;
    private List<String> types;
    private List<AreaOfLaw> areasOfLaw;
    private List<String> areasOfLawSPOE;
    private boolean displayed;
    private boolean hideAols;
    private String dxNumber;
    private Double distance;
    private List<Address> addresses;
}
