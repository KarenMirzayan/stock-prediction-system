package kz.kbtu.newsservice.config;

import kz.kbtu.common.entity.Country;
import kz.kbtu.common.entity.EconomySector;
import kz.kbtu.newsservice.repository.CountryRepository;
import kz.kbtu.newsservice.repository.EconomySectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final EconomySectorRepository sectorRepository;
    private final CountryRepository countryRepository;

    @Override
    @Transactional
    public void run(String... args) {
        initializeSectors();
        initializeCommonCountries();
    }

    private void initializeSectors() {
        if (sectorRepository.count() > 0) {
            log.info("Economy sectors already initialized");
            return;
        }

        log.info("Initializing economy sectors...");

        List<EconomySector> sectors = List.of(
            // Primary Sectors (GICS-inspired)
            EconomySector.builder()
                .code("TECH")
                .name("Technology")
                .description("Information technology, software, hardware, semiconductors, and IT services")
                .build(),
            EconomySector.builder()
                .code("HEALTHCARE")
                .name("Healthcare")
                .description("Pharmaceuticals, biotechnology, medical devices, and healthcare services")
                .build(),
            EconomySector.builder()
                .code("FINANCE")
                .name("Financial Services")
                .description("Banks, insurance, asset management, and financial technology")
                .build(),
            EconomySector.builder()
                .code("CONSUMER_DISC")
                .name("Consumer Discretionary")
                .description("Automobiles, retail, apparel, hotels, restaurants, and leisure")
                .build(),
            EconomySector.builder()
                .code("CONSUMER_STAPLES")
                .name("Consumer Staples")
                .description("Food, beverages, tobacco, and household products")
                .build(),
            EconomySector.builder()
                .code("ENERGY")
                .name("Energy")
                .description("Oil, gas, coal, and renewable energy companies")
                .build(),
            EconomySector.builder()
                .code("INDUSTRIALS")
                .name("Industrials")
                .description("Aerospace, defense, machinery, construction, and transportation")
                .build(),
            EconomySector.builder()
                .code("MATERIALS")
                .name("Materials")
                .description("Chemicals, metals, mining, and construction materials")
                .build(),
            EconomySector.builder()
                .code("REAL_ESTATE")
                .name("Real Estate")
                .description("REITs, real estate development, and property management")
                .build(),
            EconomySector.builder()
                .code("UTILITIES")
                .name("Utilities")
                .description("Electric, gas, water utilities, and independent power producers")
                .build(),
            EconomySector.builder()
                .code("TELECOM")
                .name("Telecommunications")
                .description("Wireless, wireline, and telecommunications services")
                .build(),

            // Sub-sectors for more granular classification
            EconomySector.builder()
                .code("SEMICONDUCTORS")
                .name("Semiconductors")
                .description("Semiconductor manufacturing, design, and equipment")
                .build(),
            EconomySector.builder()
                .code("SOFTWARE")
                .name("Software")
                .description("Enterprise software, cloud computing, and SaaS")
                .build(),
            EconomySector.builder()
                .code("E_COMMERCE")
                .name("E-Commerce")
                .description("Online retail, marketplaces, and digital commerce")
                .build(),
            EconomySector.builder()
                .code("SOCIAL_MEDIA")
                .name("Social Media")
                .description("Social networking platforms and digital advertising")
                .build(),
            EconomySector.builder()
                .code("AUTOMOTIVE")
                .name("Automotive")
                .description("Vehicle manufacturers, auto parts, and electric vehicles")
                .build(),
            EconomySector.builder()
                .code("AEROSPACE")
                .name("Aerospace & Defense")
                .description("Aircraft, defense systems, and space technology")
                .build(),
            EconomySector.builder()
                .code("BIOTECH")
                .name("Biotechnology")
                .description("Biotechnology research, drug development, and genomics")
                .build(),
            EconomySector.builder()
                .code("FINTECH")
                .name("Financial Technology")
                .description("Digital payments, blockchain, and financial software")
                .build(),
            EconomySector.builder()
                .code("AI_ML")
                .name("Artificial Intelligence")
                .description("AI, machine learning, and data analytics companies")
                .build(),
            EconomySector.builder()
                .code("CLEAN_ENERGY")
                .name("Clean Energy")
                .description("Solar, wind, and other renewable energy technologies")
                .build(),
            EconomySector.builder()
                .code("MEDIA")
                .name("Media & Entertainment")
                .description("Streaming, broadcasting, film, and gaming")
                .build(),
            EconomySector.builder()
                .code("CRYPTO")
                .name("Cryptocurrency")
                .description("Cryptocurrency exchanges, blockchain, and digital assets")
                .build()
        );

        sectorRepository.saveAll(sectors);
        log.info("Initialized {} economy sectors", sectors.size());
    }

    private void initializeCommonCountries() {
        if (countryRepository.count() > 0) {
            log.info("Countries already initialized");
            return;
        }

        log.info("Initializing common countries...");

        List<Country> countries = List.of(
            Country.builder().code("US").name("United States").region("North America").build(),
            Country.builder().code("CN").name("China").region("Asia").build(),
            Country.builder().code("JP").name("Japan").region("Asia").build(),
            Country.builder().code("KR").name("South Korea").region("Asia").build(),
            Country.builder().code("TW").name("Taiwan").region("Asia").build(),
            Country.builder().code("DE").name("Germany").region("Europe").build(),
            Country.builder().code("GB").name("United Kingdom").region("Europe").build(),
            Country.builder().code("FR").name("France").region("Europe").build(),
            Country.builder().code("NL").name("Netherlands").region("Europe").build(),
            Country.builder().code("CH").name("Switzerland").region("Europe").build(),
            Country.builder().code("IE").name("Ireland").region("Europe").build(),
            Country.builder().code("IN").name("India").region("Asia").build(),
            Country.builder().code("CA").name("Canada").region("North America").build(),
            Country.builder().code("AU").name("Australia").region("Oceania").build(),
            Country.builder().code("SG").name("Singapore").region("Asia").build(),
            Country.builder().code("HK").name("Hong Kong").region("Asia").build(),
            Country.builder().code("IL").name("Israel").region("Middle East").build(),
            Country.builder().code("SA").name("Saudi Arabia").region("Middle East").build(),
            Country.builder().code("AE").name("United Arab Emirates").region("Middle East").build(),
            Country.builder().code("BR").name("Brazil").region("South America").build()
        );

        countryRepository.saveAll(countries);
        log.info("Initialized {} countries", countries.size());
    }
}