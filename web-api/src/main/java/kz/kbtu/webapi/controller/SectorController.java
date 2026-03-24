package kz.kbtu.webapi.controller;

import kz.kbtu.webapi.dto.SectorHeatmapDto;
import kz.kbtu.webapi.service.SectorApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sectors")
@RequiredArgsConstructor
public class SectorController {

    private final SectorApiService sectorApiService;

    @GetMapping("/heatmap")
    public List<SectorHeatmapDto> getHeatmap(@RequestParam(defaultValue = "US") String country) {
        return sectorApiService.getHeatmap(country);
    }
}
