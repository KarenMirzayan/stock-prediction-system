package kz.kbtu.webapi.dto;

import java.util.List;

public record MarketSentimentDto(
        int score,
        String state,
        String description,
        List<String> factors,
        int totalPredictions,
        int bullishCount,
        int bearishCount,
        int neutralCount
) {}
