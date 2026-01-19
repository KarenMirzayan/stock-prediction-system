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
    private String name;
    private String description;
    private String countryCode;
    private String countryName;
    private List<String> sectorCodes;
}