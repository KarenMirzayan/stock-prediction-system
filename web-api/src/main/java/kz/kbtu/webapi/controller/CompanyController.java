package kz.kbtu.webapi.controller;

import kz.kbtu.webapi.dto.CompanyDetailDto;
import kz.kbtu.webapi.dto.CompanyListItemDto;
import kz.kbtu.webapi.service.CompanyApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyApiService companyApiService;

    @GetMapping
    public ResponseEntity<List<CompanyListItemDto>> getAllCompanies() {
        return ResponseEntity.ok(companyApiService.getAllCompanies());
    }

    @GetMapping("/{ticker}")
    public ResponseEntity<CompanyDetailDto> getCompanyDetail(@PathVariable String ticker) {
        return companyApiService.getCompanyDetail(ticker)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
