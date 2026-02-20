package kz.kbtu.webapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyDetailDto {
    private Long id;
    private String ticker;
    private String name;
    private String exchange;
    private String logoUrl;
    private String websiteUrl;
    private Double marketCap; // in millions
    private String country;   // country code, e.g. "US"
    private String countryName;
    private List<String> sectors;
    private String description;
    private String ipoDate;
}
