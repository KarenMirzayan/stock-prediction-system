package kz.kbtu.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RssArticleDto {
    private String externalId;  // CNBC article ID
    private String title;
    private String url;
    private String description;
    private LocalDateTime publishedAt;
}