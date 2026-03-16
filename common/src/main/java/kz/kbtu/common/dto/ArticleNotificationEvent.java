package kz.kbtu.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleNotificationEvent {
    private String title;
    private String summary;
    private String url;
    private Set<String> companyTickers;
}
