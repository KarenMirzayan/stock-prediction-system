package kz.kbtu.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleNotificationEvent {
    private Long articleId;
    private String title;
    private String summary;
    private Set<String> companyTickers;
    private List<String> tags;
}
