package kz.kbtu.newsservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.kbtu.common.dto.CompanyInfoDto;
import kz.kbtu.common.entity.Company;
import kz.kbtu.common.entity.Country;
import kz.kbtu.common.entity.EconomySector;
import kz.kbtu.newsservice.repository.CompanyRepository;
import kz.kbtu.newsservice.repository.CountryRepository;
import kz.kbtu.newsservice.repository.EconomySectorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CountryRepository countryRepository;
    private final EconomySectorRepository sectorRepository;
    private final TickerLookupService tickerLookupService;
    private final FinnhubService finnhubService;
    private final WikipediaService wikipediaService;
    private final ObjectMapper objectMapper;

    private final WebClient ollamaClient;

    @Value("${ollama.model:qwen2.5:14b}")
    private String model;

    public CompanyService(CompanyRepository companyRepository,
                         CountryRepository countryRepository,
                         EconomySectorRepository sectorRepository,
                         TickerLookupService tickerLookupService,
                         FinnhubService finnhubService,
                         WikipediaService wikipediaService) {
        this.companyRepository = companyRepository;
        this.countryRepository = countryRepository;
        this.sectorRepository = sectorRepository;
        this.tickerLookupService = tickerLookupService;
        this.finnhubService = finnhubService;
        this.wikipediaService = wikipediaService;
        this.objectMapper = new ObjectMapper();
        this.ollamaClient = WebClient.builder()
                .baseUrl("http://localhost:11434")
                .build();
    }

    /**
     * Resolves a company by name. Flow:
     * 1. Search DB by name (case-insensitive)
     * 2. Call Twelve Data to verify the company is publicly traded and get ticker
     * 3. Check DB by ticker (may exist under different name mention)
     * 4. Enrich with Finnhub (logo, market cap, website) + Wikipedia (description)
     * 5. Map Finnhub industry → our sector codes via LLM
     * 6. Create in DB
     */
    @Transactional
    public Company getOrCreateCompany(String companyName) {
        // 1. Check if company exists by name
        Optional<Company> existingByName = companyRepository.findByNameIgnoreCase(companyName);
        if (existingByName.isPresent()) {
            log.info("Company '{}' found in database by name", companyName);
            return existingByName.get();
        }

        // 2. Verify via Twelve Data API
        TickerLookupService.TickerResult tickerResult = tickerLookupService.lookupTicker(companyName);
        if (tickerResult == null) {
            log.warn("Company '{}' not found on any major exchange — skipping (likely private)", companyName);
            return null;
        }

        // 3. Check DB by ticker (company may exist under a different name mention)
        Optional<Company> existingByTicker = companyRepository.findByTickerIgnoreCase(tickerResult.ticker());
        if (existingByTicker.isPresent()) {
            log.info("Company '{}' resolved to ticker {} which already exists in DB as '{}'",
                    companyName, tickerResult.ticker(), existingByTicker.get().getName());
            return existingByTicker.get();
        }

        // 4. New verified public company — enrich from multiple sources
        log.info("Creating new company: '{}' → {} on {}", companyName, tickerResult.ticker(), tickerResult.exchange());
        CompanyInfoDto companyInfo = enrichCompanyData(tickerResult);

        return createCompanyFromDto(companyInfo);
    }

    @Transactional
    public List<Company> getOrCreateCompanies(List<String> companyNames) {
        List<Company> companies = new ArrayList<>();
        for (String name : companyNames) {
            Company company = getOrCreateCompany(name);
            if (company != null) {
                companies.add(company);
            }
        }
        return companies;
    }

    /**
     * Enriches company data from three sources:
     * - Twelve Data: ticker, exchange (already in tickerResult)
     * - Finnhub: logo, website, market cap, IPO date, industry
     * - Wikipedia: description
     * - LLM: maps Finnhub industry to our sector codes
     */
    private CompanyInfoDto enrichCompanyData(TickerLookupService.TickerResult tickerResult) {
        CompanyInfoDto.CompanyInfoDtoBuilder builder = CompanyInfoDto.builder()
                .ticker(tickerResult.ticker())
                .exchange(tickerResult.exchange())
                .name(tickerResult.instrumentName())
                .countryCode(mapCountryToCode(tickerResult.country()))
                .countryName(tickerResult.country());

        // Finnhub: logo, website, market cap, industry
        String finnhubIndustry = null;
        FinnhubService.CompanyProfile profile = finnhubService.getProfile(tickerResult.ticker());
        if (profile != null) {
            builder.logoUrl(profile.logoUrl())
                    .websiteUrl(profile.webUrl())
                    .marketCap(profile.marketCap())
                    .ipoDate(profile.ipoDate());
            finnhubIndustry = profile.finnhubIndustry();

            // Use Finnhub country code if available (more reliable 2-letter code)
            if (profile.country() != null && profile.country().length() == 2) {
                builder.countryCode(profile.country());
            }
        }

        // Wikipedia: description
        String description = wikipediaService.getCompanyDescription(tickerResult.instrumentName());
        if (description != null) {
            // Trim to fit DB column (2000 chars)
            if (description.length() > 2000) {
                description = description.substring(0, 1997) + "...";
            }
            builder.description(description);
        } else {
            builder.description("Publicly traded on " + tickerResult.exchange());
        }

        // LLM: map Finnhub industry to our sector codes
        List<String> sectorCodes = mapIndustryToSectors(finnhubIndustry);
        builder.sectorCodes(sectorCodes);

        return builder.build();
    }

    /**
     * Uses LLM to map a Finnhub industry string to our economy sector codes.
     * The response is tiny (~20 tokens) so truncation is not a concern.
     */
    private List<String> mapIndustryToSectors(String finnhubIndustry) {
        if (finnhubIndustry == null || finnhubIndustry.isBlank()) {
            return List.of();
        }

        List<String> availableSectors = sectorRepository.findAll().stream()
                .map(s -> s.getCode() + " (" + s.getName() + ")")
                .collect(Collectors.toList());

        String prompt = String.format("""
            Map the following industry to 1-3 sector codes from the available list.

            INDUSTRY: %s

            AVAILABLE SECTORS:
            %s

            Respond ONLY with a JSON array of sector codes. Example: ["TECH", "CONSUMER"]
            """, finnhubIndustry, String.join(", ", availableSectors));

        try {
            String response = generateFromOllama(prompt, 512);
            log.info("LLM response for sector finding: {}, industry: {}", response, finnhubIndustry);
            String cleaned = cleanJsonResponse(response);
            JsonNode root = objectMapper.readTree(cleaned);

            List<String> codes = new ArrayList<>();
            if (root.isArray()) {
                root.forEach(item -> codes.add(item.asText()));
            }
            log.info("Mapped industry '{}' → sectors: {}", finnhubIndustry, codes);
            return codes;
        } catch (Exception e) {
            log.warn("LLM failed to map industry '{}' to sectors: {}", finnhubIndustry, e.getMessage());
            return List.of();
        }
    }

    private String generateFromOllama(String prompt, int maxTokens) {
        Map<String, Object> request = Map.of(
                "model", model,
                "prompt", prompt,
                "stream", false,
                "options", Map.of(
                        "temperature", 0.1,
                        "num_predict", maxTokens,
                        "top_p", 0.9
                )
        );

        Map<String, Object> response = ollamaClient.post()
                .uri("/api/generate")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response != null && response.containsKey("response")) {
            return (String) response.get("response");
        }
        throw new RuntimeException("Invalid response from Ollama");
    }

    @Transactional
    protected Company createCompanyFromDto(CompanyInfoDto dto) {
        Country country = getOrCreateCountry(dto.getCountryCode(), dto.getCountryName());

        Set<EconomySector> sectors = new HashSet<>();
        if (dto.getSectorCodes() != null) {
            for (String sectorCode : dto.getSectorCodes()) {
                sectorRepository.findByCode(sectorCode).ifPresent(sectors::add);
            }
        }

        Company company = Company.builder()
                .ticker(dto.getTicker().toUpperCase())
                .exchange(dto.getExchange())
                .name(dto.getName())
                .description(dto.getDescription())
                .logoUrl(dto.getLogoUrl())
                .websiteUrl(dto.getWebsiteUrl())
                .marketCap(dto.getMarketCap())
                .ipoDate(dto.getIpoDate())
                .country(country)
                .sectors(sectors)
                .build();

        Company saved = companyRepository.save(company);
        log.info("Created new company: {} ({}) on {} [logo={}, sectors={}]",
                saved.getName(), saved.getTicker(), saved.getExchange(),
                saved.getLogoUrl() != null ? "yes" : "no",
                dto.getSectorCodes());

        return saved;
    }

    private Country getOrCreateCountry(String code, String name) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        return countryRepository.findByCode(code.toUpperCase())
                .orElseGet(() -> {
                    Country newCountry = Country.builder()
                            .code(code.toUpperCase())
                            .name(name != null ? name : code)
                            .build();
                    return countryRepository.save(newCountry);
                });
    }

    private String mapCountryToCode(String countryName) {
        if (countryName == null) return "US";
        return switch (countryName.toLowerCase()) {
            case "united states" -> "US";
            case "united kingdom" -> "GB";
            case "japan" -> "JP";
            case "china" -> "CN";
            case "germany" -> "DE";
            case "canada" -> "CA";
            case "australia" -> "AU";
            case "south korea" -> "KR";
            case "taiwan" -> "TW";
            case "hong kong" -> "HK";
            case "india" -> "IN";
            case "france" -> "FR";
            case "switzerland" -> "CH";
            case "singapore" -> "SG";
            case "brazil" -> "BR";
            default -> "US";
        };
    }

    private String cleanJsonResponse(String response) {
        String cleaned = response.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        }
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    public Optional<Company> findByTicker(String ticker) {
        return companyRepository.findByTickerIgnoreCase(ticker);
    }

    public boolean existsByTicker(String ticker) {
        return companyRepository.existsByTicker(ticker.toUpperCase());
    }
}
