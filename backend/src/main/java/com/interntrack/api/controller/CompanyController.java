package com.interntrack.api.controller;

import com.interntrack.api.dto.company.CompanyResponse;
import com.interntrack.api.dto.company.CreateCompanyRequest;
import com.interntrack.api.entity.Company;
import com.interntrack.api.exception.ResourceNotFoundException;
import com.interntrack.api.service.CompanyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getCompanyById(@PathVariable Long id) {
        Company company = companyService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + id));

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", CompanyResponse.from(company),
                "message", "Company retrieved successfully"
        ));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listCompanies(@RequestParam(required = false) String search) {
        List<Company> companies = (search != null && !search.isBlank())
                ? companyService.searchByName(search)
                : companyService.findAll();

        List<CompanyResponse> responseList = companies.stream()
                .map(CompanyResponse::from)
                .toList();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", responseList,
                "message", "Companies retrieved successfully"
        ));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createCompany(@Valid @RequestBody CreateCompanyRequest request) {
        Company createdCompany = companyService.createCompany(
                request.name(),
                request.domain(),
                request.industry(),
                request.logoUrl(),
                request.headquartersLocation()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "data", CompanyResponse.from(createdCompany),
                "message", "Company created successfully"
        ));
    }
}
