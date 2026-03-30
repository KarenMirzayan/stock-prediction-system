package kz.kbtu.webapi.dto;

public record CompanySentimentDto(
        int score,
        String state,
        String description,
        int totalPredictions,
        int bullishCount,
        int bearishCount,
        int neutralCount,
        RedditBuzzDto redditBuzz
) {}
