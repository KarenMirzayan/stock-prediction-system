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
import lombok.RequiredArgsConstructor;
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
    private final ObjectMapper objectMapper;
    
    private final WebClient webClient;

    private static final String OLLAMA_URL = "http://localhost:11434";

    @Value("${ollama.model:qwen2.5:14b}")
    private final String MODEL = null;

    public CompanyService(CompanyRepository companyRepository, 
                         CountryRepository countryRepository,
                         EconomySectorRepository sectorRepository) {
        this.companyRepository = companyRepository;
        this.countryRepository = countryRepository;
        this.sectorRepository = sectorRepository;
        this.objectMapper = new ObjectMapper();
        this.webClient = WebClient.builder()
                .baseUrl(OLLAMA_URL)
                .build();
    }

    @Transactional
    public Company getOrCreateCompany(String ticker) {
        // Check if company exists
        Optional<Company> existing = companyRepository.findByTickerIgnoreCase(ticker.toUpperCase());
        if (existing.isPresent()) {
            log.info("Company {} already exists in database", ticker);
            return existing.get();
        }

        // Company doesn't exist, create via LLM
        log.info("Company {} not found, generating info via LLM...", ticker);
        CompanyInfoDto companyInfo = generateCompanyInfo(ticker);
        
        if (companyInfo == null) {
            log.warn("Failed to generate company info for {}", ticker);
            return null;
        }

        return createCompanyFromDto(companyInfo);
    }

    @Transactional
    public List<Company> getOrCreateCompanies(List<String> tickers) {
        List<Company> companies = new ArrayList<>();
        
        for (String ticker : tickers) {
            Company company = getOrCreateCompany(ticker);
            if (company != null) {
                companies.add(company);
            }
        }
        
        return companies;
    }

    private CompanyInfoDto generateCompanyInfo(String ticker) {
        String prompt = buildCompanyInfoPrompt(ticker);
        
        try {
            String response = generate(prompt);
            return parseCompanyInfoResponse(response, ticker);
        } catch (Exception e) {
            log.error("Failed to generate company info for {}", ticker, e);
            return null;
        }
    }

    private String buildCompanyInfoPrompt(String ticker) {
        // Get available sectors for the prompt
        List<String> sectorCodes = sectorRepository.findAll().stream()
                .map(EconomySector::getCode)
                .collect(Collectors.toList());

        return String.format("""
            You are a financial data assistant. Provide company information for the given stock ticker.
            
            TICKER: %s
            
            AVAILABLE SECTOR CODES (choose 1-3 that apply):
            %s
            
            Respond ONLY with valid JSON. No explanations outside JSON.
            
            REQUIRED JSON FORMAT:
            {
                "ticker": "%s",
                "name": "Full company name",
                "description": "3-6 sentence description of what the company does, its main products/services, and market position",
                "countryCode": "Two-letter ISO country code (e.g., US, CN, JP, DE, KR, TW)",
                "countryName": "Full country name",
                "sectorCodes": ["SECTOR_CODE_1", "SECTOR_CODE_2"]
            }
            
            RULES:
            1. Use official company name
            2. Description should be factual, 3-6 sentences
            3. sectorCodes MUST be from the available list above
            4. If you don't know the company, respond with: {"error": "Unknown ticker"}
            """, ticker, String.join(", ", sectorCodes), ticker);
    }

    private String generate(String prompt) {
        Map<String, Object> request = Map.of(
                "model", MODEL,
                "prompt", prompt,
                "stream", false,
                "options", Map.of(
                        "temperature", 0.1,
                        "num_predict", 512,
                        "top_p", 0.9
                )
        );

        Map<String, Object> response = webClient.post()
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

    private CompanyInfoDto parseCompanyInfoResponse(String jsonResponse, String ticker) {
        try {
            String cleaned = cleanJsonResponse(jsonResponse);
            JsonNode root = objectMapper.readTree(cleaned);

            // Check for error
            if (root.has("error")) {
                log.warn("LLM returned error for ticker {}: {}", ticker, root.get("error").asText());
                return null;
            }

            return CompanyInfoDto.builder()
                    .ticker(root.path("ticker").asText(ticker).toUpperCase())
                    .name(root.path("name").asText())
                    .description(root.path("description").asText())
                    .countryCode(root.path("countryCode").asText())
                    .countryName(root.path("countryName").asText())
                    .sectorCodes(parseJsonArray(root.path("sectorCodes")))
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse company info response for {}", ticker, e);
            return null;
        }
    }

    @Transactional
    protected Company createCompanyFromDto(CompanyInfoDto dto) {
        // Get or create country
        Country country = getOrCreateCountry(dto.getCountryCode(), dto.getCountryName());

        // Get sectors
        Set<EconomySector> sectors = new HashSet<>();
        if (dto.getSectorCodes() != null) {
            for (String sectorCode : dto.getSectorCodes()) {
                sectorRepository.findByCode(sectorCode).ifPresent(sectors::add);
            }
        }

        // Create company
        Company company = Company.builder()
                .ticker(dto.getTicker().toUpperCase())
                .name(dto.getName())
                .description(dto.getDescription())
                .country(country)
                .sectors(sectors)
                .build();

        Company saved = companyRepository.save(company);
        log.info("Created new company: {} ({})", saved.getName(), saved.getTicker());
        
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

    private List<String> parseJsonArray(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node.isArray()) {
            node.forEach(item -> result.add(item.asText()));
        }
        return result;
    }

    public Optional<Company> findByTicker(String ticker) {
        return companyRepository.findByTickerIgnoreCase(ticker);
    }

    public boolean existsByTicker(String ticker) {
        return companyRepository.existsByTicker(ticker.toUpperCase());
    }
}