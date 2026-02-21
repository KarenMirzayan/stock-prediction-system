package kz.kbtu.webapi.controller;

import kz.kbtu.webapi.dto.CalendarEventDto;
import kz.kbtu.webapi.service.CalendarApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarApiService calendarApiService;

    /**
     * Upcoming events from today onward (for home page preview).
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<CalendarEventDto>> getUpcoming(
            @RequestParam(defaultValue = "6") int limit) {
        return ResponseEntity.ok(calendarApiService.getUpcomingEvents(limit));
    }

    /**
     * Events within a date range (for full calendar view).
     */
    @GetMapping
    public ResponseEntity<List<CalendarEventDto>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(calendarApiService.getEventsByDateRange(from, to));
    }
}
