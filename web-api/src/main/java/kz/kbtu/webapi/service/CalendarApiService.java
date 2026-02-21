package kz.kbtu.webapi.service;

import kz.kbtu.common.entity.MarketEvent;
import kz.kbtu.webapi.dto.CalendarEventDto;
import kz.kbtu.webapi.repository.MarketEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarApiService {

    private final MarketEventRepository marketEventRepository;

    public List<CalendarEventDto> getUpcomingEvents(int limit) {
        return marketEventRepository.findUpcoming(LocalDate.now()).stream()
                .limit(limit)
                .map(this::toDto)
                .toList();
    }

    public List<CalendarEventDto> getEventsByDateRange(LocalDate from, LocalDate to) {
        return marketEventRepository.findByDateRange(from, to).stream()
                .map(this::toDto)
                .toList();
    }

    private CalendarEventDto toDto(MarketEvent event) {
        return CalendarEventDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .date(event.getEventDate().toString())
                .time(event.getEventTime() != null ? event.getEventTime() : "TBD")
                .type(event.getType().name().toLowerCase())
                .relevance(event.getRelevance().name().toLowerCase())
                .company(event.getCompany() != null ? event.getCompany().getName() : null)
                .sector(event.getSector())
                .build();
    }
}
