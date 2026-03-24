package kz.kbtu.webapi.dto;

public record SectorHeatmapDto(
        String name,
        String code,
        double totalMarketCap,
        double weightedDailyChange,
        String sentiment
) {}
