package kz.kbtu.webapi.service;

import kz.kbtu.webapi.dto.SectorHeatmapDto;
import kz.kbtu.webapi.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SectorApiService {

    private final CompanyRepository companyRepository;

    public List<SectorHeatmapDto> getHeatmap(String countryCode) {
        return companyRepository.findSectorHeatmapByCountry(countryCode).stream()
                .map(row -> {
                    String name = (String) row[0];
                    String code = (String) row[1];
                    double totalMarketCap = ((Number) row[2]).doubleValue();
                    double weightedChange = ((Number) row[3]).doubleValue();
                    String sentiment = deriveSentiment(weightedChange);
                    return new SectorHeatmapDto(name, code, totalMarketCap, weightedChange, sentiment);
                })
                .toList();
    }

    private String deriveSentiment(double change) {
        if (change > 0.5) return "bullish";
        if (change < -0.5) return "bearish";
        return "stagnation";
    }
}
