package kz.kbtu.webapi.service;

import kz.kbtu.common.entity.Company;
import kz.kbtu.common.entity.EconomySector;
import kz.kbtu.webapi.dto.CompanyDetailDto;
import kz.kbtu.webapi.dto.CompanyListItemDto;
import kz.kbtu.webapi.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyApiService {

    private final CompanyRepository companyRepository;

    public List<CompanyListItemDto> getAllCompanies() {
        return companyRepository.findAllByOrderByNameAsc().stream()
                .map(this::toListItem)
                .toList();
    }

    public Optional<CompanyDetailDto> getCompanyDetail(String ticker) {
        return companyRepository.findByTickerIgnoreCase(ticker)
                .map(this::toDetail);
    }

    private CompanyListItemDto toListItem(Company c) {
        return CompanyListItemDto.builder()
                .id(c.getId())
                .ticker(c.getTicker())
                .name(c.getName())
                .exchange(c.getExchange())
                .logoUrl(c.getLogoUrl())
                .websiteUrl(c.getWebsiteUrl())
                .marketCap(c.getMarketCap())
                .country(c.getCountry() != null ? c.getCountry().getCode() : null)
                .sectors(c.getSectors().stream().map(EconomySector::getCode).sorted().toList())
                .build();
    }

    private CompanyDetailDto toDetail(Company c) {
        return CompanyDetailDto.builder()
                .id(c.getId())
                .ticker(c.getTicker())
                .name(c.getName())
                .exchange(c.getExchange())
                .logoUrl(c.getLogoUrl())
                .websiteUrl(c.getWebsiteUrl())
                .marketCap(c.getMarketCap())
                .country(c.getCountry() != null ? c.getCountry().getCode() : null)
                .countryName(c.getCountry() != null ? c.getCountry().getName() : null)
                .sectors(c.getSectors().stream().map(EconomySector::getCode).sorted().toList())
                .description(c.getDescription())
                .ipoDate(c.getIpoDate())
                .build();
    }
}
