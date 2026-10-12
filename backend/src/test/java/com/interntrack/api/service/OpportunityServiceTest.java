package com.interntrack.api.service;

import com.interntrack.api.entity.Company;
import com.interntrack.api.entity.Opportunity;
import com.interntrack.api.entity.User;
import com.interntrack.api.entity.enums.LocationType;
import com.interntrack.api.entity.enums.OpportunityStatus;
import com.interntrack.api.entity.enums.SourceProvider;
import com.interntrack.api.entity.enums.StipendPeriod;
import com.interntrack.api.entity.enums.UserRole;
import com.interntrack.api.exception.OpportunityAlreadyExistsException;
import com.interntrack.api.exception.ResourceNotFoundException;
import com.interntrack.api.repository.CompanyRepository;
import com.interntrack.api.repository.OpportunityRepository;
import com.interntrack.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpportunityServiceTest {

    @Mock
    private OpportunityRepository opportunityRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OpportunityService opportunityService;

    private Company sampleCompany;
    private User sampleUser;
    private Opportunity sampleOpportunity;

    @BeforeEach
    void setUp() {
        sampleCompany = new Company("Acme Corp", "acme.com", "Software");
        sampleUser = new User("user@example.com", "hash123", "Test User", UserRole.ROLE_USER);
        sampleOpportunity = new Opportunity(sampleCompany, "Software Engineering Intern", LocationType.REMOTE, SourceProvider.SYSTEM);
    }

    @Test
    @DisplayName("findById returns opportunity when present")
    void findById_whenFound_returnsOpportunity() {
        when(opportunityRepository.findById(1L)).thenReturn(Optional.of(sampleOpportunity));

        Optional<Opportunity> result = opportunityService.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Software Engineering Intern");
        verify(opportunityRepository).findById(1L);
    }

    @Test
    @DisplayName("findById returns empty when null")
    void findById_whenNull_returnsEmpty() {
        Optional<Opportunity> result = opportunityService.findById(null);
        assertThat(result).isEmpty();
        verifyNoInteractions(opportunityRepository);
    }

    @Test
    @DisplayName("findAccessibleById returns opportunity when accessible")
    void findAccessibleById_whenAccessible_returnsOpportunity() {
        when(opportunityRepository.findAccessibleById(1L, 10L)).thenReturn(Optional.of(sampleOpportunity));

        Optional<Opportunity> result = opportunityService.findAccessibleById(1L, 10L);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(sampleOpportunity);
        verify(opportunityRepository).findAccessibleById(1L, 10L);
    }

    @Test
    @DisplayName("findAccessibleById returns empty when null id or user")
    void findAccessibleById_whenNullArgs_returnsEmpty() {
        assertThat(opportunityService.findAccessibleById(null, 10L)).isEmpty();
        assertThat(opportunityService.findAccessibleById(1L, null)).isEmpty();
        verifyNoInteractions(opportunityRepository);
    }

    @Test
    @DisplayName("findAccessibleOpportunities delegates to repository with trimmed search")
    void findAccessibleOpportunities_delegatesWithTrimmedSearch() {
        when(opportunityRepository.findAccessibleOpportunities(10L, LocationType.REMOTE, "java"))
                .thenReturn(List.of(sampleOpportunity));

        List<Opportunity> result = opportunityService.findAccessibleOpportunities(10L, LocationType.REMOTE, "  java  ");

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(sampleOpportunity);
        verify(opportunityRepository).findAccessibleOpportunities(10L, LocationType.REMOTE, "java");
    }

    @Test
    @DisplayName("findAccessibleOpportunities returns empty when null userId")
    void findAccessibleOpportunities_whenNullUserId_returnsEmpty() {
        List<Opportunity> result = opportunityService.findAccessibleOpportunities(null, null, null);
        assertThat(result).isEmpty();
        verifyNoInteractions(opportunityRepository);
    }

    @Test
    @DisplayName("findByProviderAndExternalId returns opportunity when present")
    void findByProviderAndExternalId_whenPresent_returnsOpportunity() {
        when(opportunityRepository.findBySourceProviderAndExternalSourceId(SourceProvider.SYSTEM, "ext-1"))
                .thenReturn(Optional.of(sampleOpportunity));

        Optional<Opportunity> result = opportunityService.findByProviderAndExternalId(SourceProvider.SYSTEM, "ext-1");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(sampleOpportunity);
    }

    @Test
    @DisplayName("findByProviderAndExternalId returns empty on null or blank")
    void findByProviderAndExternalId_whenNullOrBlank_returnsEmpty() {
        assertThat(opportunityService.findByProviderAndExternalId(null, "ext-1")).isEmpty();
        assertThat(opportunityService.findByProviderAndExternalId(SourceProvider.SYSTEM, null)).isEmpty();
        assertThat(opportunityService.findByProviderAndExternalId(SourceProvider.SYSTEM, "   ")).isEmpty();
        verifyNoInteractions(opportunityRepository);
    }

    @Test
    @DisplayName("findByRawHash returns opportunity when present")
    void findByRawHash_whenPresent_returnsOpportunity() {
        when(opportunityRepository.findByRawHash("hash123")).thenReturn(Optional.of(sampleOpportunity));

        Optional<Opportunity> result = opportunityService.findByRawHash("hash123");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(sampleOpportunity);
    }

    @Test
    @DisplayName("findByRawHash returns empty on null or blank")
    void findByRawHash_whenNullOrBlank_returnsEmpty() {
        assertThat(opportunityService.findByRawHash(null)).isEmpty();
        assertThat(opportunityService.findByRawHash("  ")).isEmpty();
        verifyNoInteractions(opportunityRepository);
    }

    @Test
    @DisplayName("findByStatus returns matching opportunities")
    void findByStatus_whenValid_returnsList() {
        when(opportunityRepository.findByStatus(OpportunityStatus.ACTIVE)).thenReturn(List.of(sampleOpportunity));

        List<Opportunity> result = opportunityService.findByStatus(OpportunityStatus.ACTIVE);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("findByStatus returns empty list on null")
    void findByStatus_whenNull_returnsEmptyList() {
        assertThat(opportunityService.findByStatus(null)).isEmpty();
        verifyNoInteractions(opportunityRepository);
    }

    @Test
    @DisplayName("createOpportunity creates system opportunity when createdByUserId is null")
    void createOpportunity_systemOpportunity_createsWithNullCreator() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(sampleCompany));
        when(opportunityRepository.save(any(Opportunity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Opportunity created = opportunityService.createOpportunity(
                1L,
                null,
                SourceProvider.SYSTEM,
                null,
                "Backend Intern",
                "Description",
                LocationType.REMOTE,
                null,
                new BigDecimal("40.00"),
                "USD",
                StipendPeriod.HOURLY,
                3,
                null,
                "https://example.com/job",
                null
        );

        assertThat(created).isNotNull();
        assertThat(created.getTitle()).isEqualTo("Backend Intern");
        assertThat(created.getCreatedByUser()).isNull();
        assertThat(created.getSourceProvider()).isEqualTo(SourceProvider.SYSTEM);

        ArgumentCaptor<Opportunity> captor = ArgumentCaptor.forClass(Opportunity.class);
        verify(opportunityRepository).save(captor.capture());
        assertThat(captor.getValue().getCreatedByUser()).isNull();
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("createOpportunity creates user custom opportunity when createdByUserId is provided")
    void createOpportunity_userCustomOpportunity_createsWithAssociatedCreator() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(sampleCompany));
        when(userRepository.findById(10L)).thenReturn(Optional.of(sampleUser));
        when(opportunityRepository.save(any(Opportunity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Opportunity created = opportunityService.createOpportunity(
                1L,
                10L,
                SourceProvider.USER_MANUAL,
                null,
                "Custom Local Internship",
                "Notes",
                LocationType.ONSITE,
                "Chicago",
                new BigDecimal("30.00"),
                "USD",
                StipendPeriod.HOURLY,
                6,
                null,
                null,
                null
        );

        assertThat(created).isNotNull();
        assertThat(created.getCreatedByUser()).isEqualTo(sampleUser);
        assertThat(created.getSourceProvider()).isEqualTo(SourceProvider.USER_MANUAL);

        ArgumentCaptor<Opportunity> captor = ArgumentCaptor.forClass(Opportunity.class);
        verify(opportunityRepository).save(captor.capture());
        assertThat(captor.getValue().getCreatedByUser()).isEqualTo(sampleUser);
    }

    @Test
    @DisplayName("createOpportunity throws OpportunityAlreadyExistsException on duplicate provider/external ID")
    void createOpportunity_whenDuplicateProviderExternalId_throwsException() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(sampleCompany));
        when(opportunityRepository.findBySourceProviderAndExternalSourceId(SourceProvider.ARBEITSNOW, "ext_999"))
                .thenReturn(Optional.of(sampleOpportunity));

        assertThatThrownBy(() -> opportunityService.createOpportunity(
                1L,
                null,
                SourceProvider.ARBEITSNOW,
                "ext_999",
                "Duplicate Role",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ))
                .isInstanceOf(OpportunityAlreadyExistsException.class)
                .hasMessageContaining("ARBEITSNOW")
                .hasMessageContaining("ext_999");

        verify(opportunityRepository, never()).save(any());
    }

    @Test
    @DisplayName("createOpportunity throws ResourceNotFoundException when company not found")
    void createOpportunity_whenCompanyNotFound_throwsException() {
        when(companyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> opportunityService.createOpportunity(
                999L,
                null,
                SourceProvider.SYSTEM,
                null,
                "Role Title",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Company not found");

        verify(opportunityRepository, never()).save(any());
    }

    @Test
    @DisplayName("createOpportunity throws ResourceNotFoundException when creator user not found")
    void createOpportunity_whenCreatorUserNotFound_throwsException() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(sampleCompany));
        when(userRepository.findById(888L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> opportunityService.createOpportunity(
                1L,
                888L,
                SourceProvider.USER_MANUAL,
                null,
                "Role Title",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Creator user not found");

        verify(opportunityRepository, never()).save(any());
    }

    @Test
    @DisplayName("createOpportunity throws IllegalArgumentException on blank title or null companyId")
    void createOpportunity_whenInvalidArguments_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> opportunityService.createOpportunity(
                null, null, SourceProvider.SYSTEM, null, "Valid Title", null, null, null, null, null, null, null, null, null, null
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> opportunityService.createOpportunity(
                1L, null, SourceProvider.SYSTEM, null, "   ", null, null, null, null, null, null, null, null, null, null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("updateOpportunity updates mutable fields when requested by creator")
    void updateOpportunity_whenCreatorRequests_updatesFields() {
        Opportunity customOpp = new Opportunity(sampleCompany, "Old Title", LocationType.REMOTE, SourceProvider.USER_MANUAL);
        User creator = mock(User.class);
        when(creator.getId()).thenReturn(10L);
        customOpp.setCreatedByUser(creator);

        when(opportunityRepository.findById(1L)).thenReturn(Optional.of(customOpp));
        when(opportunityRepository.save(any(Opportunity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Opportunity updated = opportunityService.updateOpportunity(
                1L,
                10L, // Creator matches
                "New Title",
                "New Description",
                LocationType.HYBRID,
                "Austin, TX",
                new BigDecimal("50.00"),
                "USD",
                StipendPeriod.HOURLY,
                4,
                null,
                "https://new.url",
                null
        );

        assertThat(updated.getTitle()).isEqualTo("New Title");
        assertThat(updated.getDescription()).isEqualTo("New Description");
        assertThat(updated.getLocationType()).isEqualTo(LocationType.HYBRID);
        assertThat(updated.getLocationCity()).isEqualTo("Austin, TX");
        assertThat(updated.getStipendAmount()).isEqualByComparingTo("50.00");
        assertThat(updated.getSourceProvider()).isEqualTo(SourceProvider.USER_MANUAL);
        assertThat(updated.getCompany()).isEqualTo(sampleCompany);
    }

    @Test
    @DisplayName("updateOpportunity throws AccessDeniedException when unauthorized user attempts to edit custom opportunity")
    void updateOpportunity_whenUnauthorizedUser_throwsAccessDeniedException() {
        Opportunity customOpp = new Opportunity(sampleCompany, "Custom Opp", LocationType.REMOTE, SourceProvider.USER_MANUAL);
        User creator = mock(User.class);
        when(creator.getId()).thenReturn(10L);
        customOpp.setCreatedByUser(creator);

        when(opportunityRepository.findById(1L)).thenReturn(Optional.of(customOpp));

        assertThatThrownBy(() -> opportunityService.updateOpportunity(
                1L,
                99L, // Different user ID
                "Hacked Title",
                null, null, null, null, null, null, null, null, null, null
        ))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Unauthorized: Only the creator may modify this custom opportunity");

        verify(opportunityRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateOpportunity throws AccessDeniedException when standard user attempts to edit global opportunity")
    void updateOpportunity_whenGlobalOpportunity_throwsAccessDeniedException() {
        Opportunity globalOpp = new Opportunity(sampleCompany, "Global Opp", LocationType.REMOTE, SourceProvider.SYSTEM);
        // createdByUser is null for global opportunity
        when(opportunityRepository.findById(1L)).thenReturn(Optional.of(globalOpp));

        assertThatThrownBy(() -> opportunityService.updateOpportunity(
                1L,
                10L,
                "Hacked Global Title",
                null, null, null, null, null, null, null, null, null, null
        ))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Cannot modify global system opportunities");

        verify(opportunityRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateOpportunity throws ResourceNotFoundException on non-existent ID")
    void updateOpportunity_whenNotFound_throwsResourceNotFoundException() {
        when(opportunityRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> opportunityService.updateOpportunity(
                999L,
                10L,
                "Title",
                null, null, null, null, null, null, null, null, null, null
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Opportunity not found with id: 999");
    }

    @Test
    @DisplayName("updateOpportunity throws IllegalArgumentException on null ID")
    void updateOpportunity_whenNullId_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> opportunityService.updateOpportunity(
                null, 10L, "Title", null, null, null, null, null, null, null, null, null, null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("deleteOpportunity deletes and flushes when creator requests")
    void deleteOpportunity_whenCreatorRequests_deletesAndFlushes() {
        Opportunity customOpp = new Opportunity(sampleCompany, "Custom Opp", LocationType.REMOTE, SourceProvider.USER_MANUAL);
        User creator = mock(User.class);
        when(creator.getId()).thenReturn(10L);
        customOpp.setCreatedByUser(creator);

        when(opportunityRepository.findById(1L)).thenReturn(Optional.of(customOpp));

        opportunityService.deleteOpportunity(1L, 10L);

        verify(opportunityRepository).delete(customOpp);
        verify(opportunityRepository).flush();
    }

    @Test
    @DisplayName("deleteOpportunity throws AccessDeniedException when unauthorized user attempts delete")
    void deleteOpportunity_whenUnauthorizedUser_throwsAccessDeniedException() {
        Opportunity customOpp = new Opportunity(sampleCompany, "Custom Opp", LocationType.REMOTE, SourceProvider.USER_MANUAL);
        User creator = mock(User.class);
        when(creator.getId()).thenReturn(10L);
        customOpp.setCreatedByUser(creator);

        when(opportunityRepository.findById(1L)).thenReturn(Optional.of(customOpp));

        assertThatThrownBy(() -> opportunityService.deleteOpportunity(1L, 99L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Unauthorized: Only the creator may delete this custom opportunity");

        verify(opportunityRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteOpportunity throws AccessDeniedException when standard user attempts delete on global opportunity")
    void deleteOpportunity_whenGlobalOpportunity_throwsAccessDeniedException() {
        Opportunity globalOpp = new Opportunity(sampleCompany, "Global Opp", LocationType.REMOTE, SourceProvider.SYSTEM);
        when(opportunityRepository.findById(1L)).thenReturn(Optional.of(globalOpp));

        assertThatThrownBy(() -> opportunityService.deleteOpportunity(1L, 10L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Cannot delete global system opportunities");

        verify(opportunityRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteOpportunity throws ResourceNotFoundException on non-existent ID")
    void deleteOpportunity_whenNotFound_throwsResourceNotFoundException() {
        when(opportunityRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> opportunityService.deleteOpportunity(999L, 10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Opportunity not found with id: 999");
    }

    @Test
    @DisplayName("deleteOpportunity throws IllegalArgumentException on null ID")
    void deleteOpportunity_whenNullId_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> opportunityService.deleteOpportunity(null, 10L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
