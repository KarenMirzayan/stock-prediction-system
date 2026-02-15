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
            EconomySector.builder()
                .code("TECH")
                .name("Technology")
                .description("Software, hardware, semiconductors, AI, cloud, and IT services")
                .build(),
            EconomySector.builder()
                .code("HEALTHCARE")
                .name("Healthcare")
                .description("Pharmaceuticals, biotechnology, medical devices, and healthcare services")
                .build(),
            EconomySector.builder()
                .code("FINANCE")
                .name("Financial Services")
                .description("Banks, insurance, asset management, fintech, and crypto")
                .build(),
            EconomySector.builder()
                .code("CONSUMER")
                .name("Consumer")
                .description("Retail, e-commerce, food, beverages, apparel, media, and entertainment")
                .build(),
            EconomySector.builder()
                .code("ENERGY")
                .name("Energy")
                .description("Oil, gas, coal, renewables, clean energy, and utilities")
                .build(),
            EconomySector.builder()
                .code("INDUSTRIALS")
                .name("Industrials")
                .description("Aerospace, defense, machinery, construction, automotive, and transportation")
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
            Country.builder().code("BR").name("Brazil").region("South America").build(),
            Country.builder().code("KZ").name("Kazakhstan").region("Asia").build()
        );

        countryRepository.saveAll(countries);
        log.info("Initialized {} countries", countries.size());
    }
}