package kz.kbtu.webapi.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarEventDto {
    private Long id;
    private String title;
    private String date;       // "2026-02-20"
    private String time;       // "4:30 PM ET" or "TBD"
    private String type;       // earnings | economic | dividend | conference
    private String relevance;  // high | medium | low
    private String company;    // company name or null
    private String sector;
}
