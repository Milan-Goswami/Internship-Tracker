package com.interntrack.api.service;

import com.interntrack.api.entity.Company;
import com.interntrack.api.exception.CompanyAlreadyExistsException;
import com.interntrack.api.repository.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private CompanyService companyService;

    private Company sampleCompany;

    @BeforeEach
    void setUp() {
        sampleCompany = new Company("Google LLC", "google.com", "Technology");
    }

    @Test
    @DisplayName("findById returns company when found")
    void findById_whenFound_returnsCompany() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(sampleCompany));

        Optional<Company> result = companyService.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Google LLC");
        verify(companyRepository).findById(1L);
    }

    @Test
    @DisplayName("findById returns empty when not found or id is null")
    void findById_whenNotFound_returnsEmpty() {
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(companyService.findById(99L)).isEmpty();
        assertThat(companyService.findById(null)).isEmpty();
    }

    @Test
    @DisplayName("findByCanonicalName normalizes query and returns company")
    void findByCanonicalName_whenFound_returnsCompany() {
        when(companyRepository.findByCanonicalName("googlellc")).thenReturn(Optional.of(sampleCompany));

        Optional<Company> result = companyService.findByCanonicalName("  Google, LLC! ");

        assertThat(result).isPresent();
        assertThat(result.get().getCanonicalName()).isEqualTo("googlellc");
        verify(companyRepository).findByCanonicalName("googlellc");
    }

    @Test
    @DisplayName("findByCanonicalName returns empty on blank input without querying repo")
    void findByCanonicalName_whenBlank_returnsEmpty() {
        assertThat(companyService.findByCanonicalName(null)).isEmpty();
        assertThat(companyService.findByCanonicalName("   ")).isEmpty();
        verifyNoInteractions(companyRepository);
    }

    @Test
    @DisplayName("existsByCanonicalName returns true when company exists")
    void existsByCanonicalName_whenExists_returnsTrue() {
        when(companyRepository.existsByCanonicalName("googlellc")).thenReturn(true);

        boolean exists = companyService.existsByCanonicalName("Google LLC");

        assertThat(exists).isTrue();
        verify(companyRepository).existsByCanonicalName("googlellc");
    }

    @Test
    @DisplayName("existsByCanonicalName returns false when company does not exist")
    void existsByCanonicalName_whenDoesNotExist_returnsFalse() {
        when(companyRepository.existsByCanonicalName("nonexistent")).thenReturn(false);

        boolean exists = companyService.existsByCanonicalName("Non Existent");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("findAll delegates to repository.findAll")
    void findAll_returnsAllCompanies() {
        when(companyRepository.findAll()).thenReturn(List.of(sampleCompany));

        List<Company> results = companyService.findAll();

        assertThat(results).hasSize(1).contains(sampleCompany);
        verify(companyRepository).findAll();
    }

    @Test
    @DisplayName("searchByName with valid query queries repository")
    void searchByName_withQuery_returnsMatchingCompanies() {
        when(companyRepository.findByNameContainingIgnoreCase("google")).thenReturn(List.of(sampleCompany));

        List<Company> results = companyService.searchByName("  google  ");

        assertThat(results).hasSize(1).contains(sampleCompany);
        verify(companyRepository).findByNameContainingIgnoreCase("google");
    }

    @Test
    @DisplayName("searchByName with blank or null query returns findAll")
    void searchByName_withBlankQuery_returnsAll() {
        when(companyRepository.findAll()).thenReturn(List.of(sampleCompany));

        List<Company> nullResult = companyService.searchByName(null);
        List<Company> blankResult = companyService.searchByName("   ");

        assertThat(nullResult).hasSize(1);
        assertThat(blankResult).hasSize(1);
        verify(companyRepository, times(2)).findAll();
        verify(companyRepository, never()).findByNameContainingIgnoreCase(any());
    }

    @Test
    @DisplayName("createCompany successfully normalizes canonicalName and saves company via saveAndFlush")
    void createCompany_whenValidInput_createsAndSaves() {
        when(companyRepository.existsByCanonicalName("stripeinc")).thenReturn(false);
        when(companyRepository.saveAndFlush(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Company created = companyService.createCompany(
                "  Stripe, Inc.  ",
                "  STRIPE.COM  ",
                "  Fintech  ",
                "https://logo.clearbit.com/stripe.com",
                "San Francisco, CA"
        );

        assertThat(created).isNotNull();
        assertThat(created.getName()).isEqualTo("Stripe, Inc.");
        assertThat(created.getCanonicalName()).isEqualTo("stripeinc");
        assertThat(created.getDomain()).isEqualTo("stripe.com");
        assertThat(created.getIndustry()).isEqualTo("Fintech");
        assertThat(created.getLogoUrl()).isEqualTo("https://logo.clearbit.com/stripe.com");
        assertThat(created.getHeadquartersLocation()).isEqualTo("San Francisco, CA");

        ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
        verify(companyRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getCanonicalName()).isEqualTo("stripeinc");
    }

    @Test
    @DisplayName("createCompany throws CompanyAlreadyExistsException on application-level duplicate canonical name")
    void createCompany_whenDuplicate_throwsException() {
        when(companyRepository.existsByCanonicalName("googlellc")).thenReturn(true);

        assertThatThrownBy(() -> companyService.createCompany(
                "Google LLC",
                "google.com",
                "Technology",
                null,
                null
        ))
                .isInstanceOf(CompanyAlreadyExistsException.class)
                .hasMessageContaining("googlellc");

        verify(companyRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("createCompany when saveAndFlush throws DataIntegrityViolationException on uk_company_canonical translates to CompanyAlreadyExistsException")
    void createCompany_whenDatabaseConstraintViolation_translatesToCompanyAlreadyExistsException() {
        when(companyRepository.existsByCanonicalName("googlellc")).thenReturn(false);
        SQLException sqlEx = new SQLException("Duplicate entry 'googlellc' for key 'companies.uk_company_canonical'");
        when(companyRepository.saveAndFlush(any(Company.class)))
                .thenThrow(new DataIntegrityViolationException("Constraint violation", sqlEx));

        assertThatThrownBy(() -> companyService.createCompany(
                "Google LLC",
                "google.com",
                "Technology",
                null,
                null
        ))
                .isInstanceOf(CompanyAlreadyExistsException.class)
                .hasMessageContaining("googlellc");
    }

    @Test
    @DisplayName("createCompany when saveAndFlush throws unrelated DataIntegrityViolationException rethrows original exception")
    void createCompany_whenUnrelatedDataIntegrityViolation_rethrows() {
        when(companyRepository.existsByCanonicalName("googlellc")).thenReturn(false);
        SQLException sqlEx = new SQLException("General database error");
        when(companyRepository.saveAndFlush(any(Company.class)))
                .thenThrow(new DataIntegrityViolationException("Constraint violation", sqlEx));

        assertThatThrownBy(() -> companyService.createCompany(
                "Google LLC",
                "google.com",
                "Technology",
                null,
                null
        ))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("createCompany throws IllegalArgumentException on blank or invalid names")
    void createCompany_whenInvalidName_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> companyService.createCompany("", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> companyService.createCompany("   ", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> companyService.createCompany("!!! ###", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("alphanumeric");

        verifyNoInteractions(companyRepository);
    }

    @Test
    @DisplayName("normalizeCanonicalName produces deterministic lowercase alphanumeric output")
    void normalizeCanonicalName_producesDeterministicOutput() {
        assertThat(companyService.normalizeCanonicalName("Google, Inc.")).isEqualTo("googleinc");
        assertThat(companyService.normalizeCanonicalName("  Stripe-USA (Holdings)  ")).isEqualTo("stripeusaholdings");
        assertThat(companyService.normalizeCanonicalName("A&B Solutions 123")).isEqualTo("absolutions123");
        assertThat(companyService.normalizeCanonicalName(null)).isNull();
    }
}