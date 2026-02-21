package kz.kbtu.newsservice.service;

import kz.kbtu.common.dto.MarketEventDto;
import kz.kbtu.common.entity.Article;
import kz.kbtu.common.entity.Company;
import kz.kbtu.common.entity.EconomySector;
import kz.kbtu.common.entity.MarketEvent;
import kz.kbtu.newsservice.repository.CompanyRepository;
import kz.kbtu.newsservice.repository.EconomySectorRepository;
import kz.kbtu.newsservice.repository.MarketEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketEventService {

    private final MarketEventRepository eventRepository;
    private final CompanyRepository companyRepository;
    private final EconomySectorRepository sectorRepository;

    @Transactional
    public void saveEvents(List<MarketEventDto> dtos, Article sourceArticle) {
        for (MarketEventDto dto : dtos) {
            if (dto.getTitle() == null || dto.getDate() == null) continue;

            LocalDate eventDate;
            try {
                eventDate = LocalDate.parse(dto.getDate());
            } catch (DateTimeParseException e) {
                log.warn("Invalid date '{}' for event '{}', skipping", dto.getDate(), dto.getTitle());
                continue;
            }

            // Skip past events
            if (eventDate.isBefore(LocalDate.now())) {
                log.debug("Skipping past event: {} on {}", dto.getTitle(), eventDate);
                continue;
            }

            // Deduplicate by title + date
            if (eventRepository.existsByTitleIgnoreCaseAndEventDate(dto.getTitle(), eventDate)) {
                log.debug("Skipping duplicate event: {} on {}", dto.getTitle(), eventDate);
                continue;
            }

            Company company = null;
            if (dto.getCompanyTicker() != null && !dto.getCompanyTicker().isBlank()) {
                company = companyRepository.findByTickerIgnoreCase(dto.getCompanyTicker()).orElse(null);
                if (company == null) {
                    log.debug("Ticker '{}' not found in DB for event '{}', saving without company link",
                            dto.getCompanyTicker(), dto.getTitle());
                }
            }

            // Resolve sector name against DB (try by name first, then by code)
            final String rawSector = dto.getSector();
            String sectorName = rawSector;
            if (rawSector != null && !rawSector.isBlank()) {
                sectorName = sectorRepository.findByNameIgnoreCase(rawSector)
                        .or(() -> sectorRepository.findByCode(rawSector.toUpperCase()))
                        .map(EconomySector::getName)
                        .orElse(rawSector);
            }

            MarketEvent event = MarketEvent.builder()
                    .title(dto.getTitle())
                    .eventDate(eventDate)
                    .eventTime(dto.getTime())
                    .type(parseType(dto.getType()))
                    .relevance(parseRelevance(dto.getRelevance()))
                    .sector(sectorName)
                    .company(company)
                    .sourceArticle(sourceArticle)
                    .build();

            eventRepository.save(event);
            log.info("Saved market event: {} ({}) on {}", dto.getTitle(), dto.getType(), eventDate);
        }
    }

    private MarketEvent.EventType parseType(String raw) {
        if (raw == null) return MarketEvent.EventType.ECONOMIC;
        try {
            return MarketEvent.EventType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return MarketEvent.EventType.ECONOMIC;
        }
    }

    private MarketEvent.Relevance parseRelevance(String raw) {
        if (raw == null) return MarketEvent.Relevance.MEDIUM;
        try {
            return MarketEvent.Relevance.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return MarketEvent.Relevance.MEDIUM;
        }
    }
}
