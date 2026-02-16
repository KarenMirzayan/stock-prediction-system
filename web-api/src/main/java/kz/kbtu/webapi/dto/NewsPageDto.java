package kz.kbtu.webapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsPageDto {
    private List<NewsItemDto> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
