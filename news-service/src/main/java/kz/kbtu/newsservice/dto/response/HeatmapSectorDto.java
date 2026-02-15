package kz.kbtu.newsservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class HeatmapSectorDto {
    private String id;
    private String name;
    private int sentiment;
    private int discussionVolume;
    private List<HeatmapCompanyDto> companies;
    private List<String> topics;
}
