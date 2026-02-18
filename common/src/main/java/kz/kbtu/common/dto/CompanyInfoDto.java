package kz.kbtu.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyInfoDto {
    private String ticker;
    private String exchange;
    private String name;
    private String description;
    private String logoUrl;
    private String websiteUrl;
    private Double marketCap;
    private String ipoDate;
    private String countryCode;
    private String countryName;
    private List<String> sectorCodes;
}