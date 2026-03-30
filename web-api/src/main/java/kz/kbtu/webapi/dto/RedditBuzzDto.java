package kz.kbtu.webapi.dto;

public record RedditBuzzDto(
        int rank,
        int mentions,
        int upvotes,
        Integer rank24hAgo,
        Integer mentions24hAgo,
        int rankChange,
        int mentionChange
) {}
