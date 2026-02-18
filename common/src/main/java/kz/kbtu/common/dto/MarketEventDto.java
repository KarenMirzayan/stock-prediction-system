package kz.kbtu.common.dto;

import lombok.*;

/**
 * Represents a single calendar event extracted by the LLM from a news article.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketEventDto {

    private String title;

    /** ISO date string: "2026-02-20" */
    private String date;

    /** e.g. "4:30 PM ET" or "TBD" */
    private String time;

    /** EARNINGS | ECONOMIC | DIVIDEND | CONFERENCE */
    private String type;

    /** HIGH | MEDIUM | LOW */
    private String relevance;

    /** Ticker symbol, null for macro events */
    private String companyTicker;

    private String sector;
}
