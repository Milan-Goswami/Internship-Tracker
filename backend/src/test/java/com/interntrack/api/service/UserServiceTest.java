package com.interntrack.api.service;

import com.interntrack.api.dto.user.UpdateUserProfilePreferencesRequest;
import com.interntrack.api.dto.user.UserProfileResponse;
import com.interntrack.api.entity.User;
import com.interntrack.api.entity.UserProfile;
import com.interntrack.api.entity.enums.PreferredWorkMode;
import com.interntrack.api.entity.enums.UserRole;
import com.interntrack.api.exception.ResourceNotFoundException;
import com.interntrack.api.exception.UserAlreadyExistsException;
import com.interntrack.api.repository.UserProfileRepository;
import com.interntrack.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private UserService userService;

    private User sampleUser;
    private Instant sampleCreatedAt;

    @BeforeEach
    void setUp() {
        sampleCreatedAt = Instant.now().minusSeconds(3600);
        sampleUser = new User("candidate@example.com", "$2a$12$someHash", "Alice Candidate", UserRole.ROLE_USER);
        ReflectionTestUtils.setField(sampleUser, "id", 1L);
        ReflectionTestUtils.setField(sampleUser, "createdAt", sampleCreatedAt);
        sampleUser.setIsActive(true);
    }

    @Test
    @DisplayName("Email normalization converts to lowercase and trims whitespace")
    void normalizeEmail_formatsCorrectly() {
        assertThat(userService.normalizeEmail("  Alice.Candidate@EXAMPLE.COM  ")).isEqualTo("alice.candidate@example.com");
        assertThat(userService.normalizeEmail(null)).isNull();
    }

    @Test
    @DisplayName("Create user with unique email succeeds")
    void createUser_success() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User created = userService.createUser("Alice@EXAMPLE.com", "$2a$12$hash", "Alice", UserRole.ROLE_USER);

        assertThat(created.getEmail()).isEqualTo("alice@example.com");
        assertThat(created.getFullName()).isEqualTo("Alice");
        assertThat(created.getRole()).isEqualTo(UserRole.ROLE_USER);
    }

    @Test
    @DisplayName("Create user with existing email throws UserAlreadyExistsException")
    void createUser_duplicateEmail_throwsException() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser("Alice@example.com", "$2a$12$hash", "Alice", UserRole.ROLE_USER))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("getUserProfile when no profile row exists returns user identity with default preferences")
    void getUserProfile_noProfileRow_returnsDefaultsWithoutFailing() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userProfileRepository.findById(1L)).thenReturn(Optional.empty());

        UserProfileResponse res = userService.getUserProfile(1L);

        assertThat(res.id()).isEqualTo(1L);
        assertThat(res.email()).isEqualTo("candidate@example.com");
        assertThat(res.fullName()).isEqualTo("Alice Candidate");
        assertThat(res.targetTitle()).isNull();
        assertThat(res.targetLocation()).isNull();
        assertThat(res.targetSalaryMin()).isNull();
        assertThat(res.targetSalaryMax()).isNull();
        assertThat(res.preferredWorkMode()).isEqualTo(PreferredWorkMode.ANY);
    }

    @Test
    @DisplayName("getUserProfile when profile row exists returns identity and persisted preferences")
    void getUserProfile_withProfileRow_returnsPreferences() {
        UserProfile profile = new UserProfile(sampleUser, "Software Engineer Intern", "New York, NY", BigDecimal.valueOf(45.00), BigDecimal.valueOf(65.00), PreferredWorkMode.HYBRID);

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userProfileRepository.findById(1L)).thenReturn(Optional.of(profile));

        UserProfileResponse res = userService.getUserProfile(1L);

        assertThat(res.id()).isEqualTo(1L);
        assertThat(res.targetTitle()).isEqualTo("Software Engineer Intern");
        assertThat(res.targetLocation()).isEqualTo("New York, NY");
        assertThat(res.targetSalaryMin()).isEqualTo(BigDecimal.valueOf(45.00));
        assertThat(res.targetSalaryMax()).isEqualTo(BigDecimal.valueOf(65.00));
        assertThat(res.preferredWorkMode()).isEqualTo(PreferredWorkMode.HYBRID);
    }

    @Test
    @DisplayName("Update user profile with valid fullName succeeds and preserves all immutable fields")
    void updateProfile_validFullName_preservesImmutableFields() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(userProfileRepository.findById(1L)).thenReturn(Optional.empty());

        UserProfileResponse updated = userService.updateProfile(1L, "Alice Updated");

        assertThat(updated.fullName()).isEqualTo("Alice Updated");
        assertThat(updated.id()).isEqualTo(1L);
        assertThat(updated.email()).isEqualTo("candidate@example.com");
        assertThat(updated.role()).isEqualTo(UserRole.ROLE_USER);
        assertThat(updated.isActive()).isTrue();
        assertThat(updated.createdAt()).isEqualTo(sampleCreatedAt);

        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("updatePreferences creates new profile row when absent")
    void updatePreferences_whenAbsent_createsProfile() {
        UpdateUserProfilePreferencesRequest req = new UpdateUserProfilePreferencesRequest(
                "Alice Named", "SWE Intern", "Remote", BigDecimal.valueOf(50.00), BigDecimal.valueOf(70.00), PreferredWorkMode.REMOTE
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(userProfileRepository.findById(1L)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));

        UserProfileResponse res = userService.updatePreferences(1L, req);

        assertThat(res.fullName()).isEqualTo("Alice Named");
        assertThat(res.targetTitle()).isEqualTo("SWE Intern");
        assertThat(res.targetLocation()).isEqualTo("Remote");
        assertThat(res.targetSalaryMin()).isEqualTo(BigDecimal.valueOf(50.00));
        assertThat(res.targetSalaryMax()).isEqualTo(BigDecimal.valueOf(70.00));
        assertThat(res.preferredWorkMode()).isEqualTo(PreferredWorkMode.REMOTE);
        verify(userProfileRepository).save(any(UserProfile.class));
    }

    @Test
    @DisplayName("updatePreferences with invalid salary range (min > max) throws IllegalArgumentException")
    void updatePreferences_minGreaterThanMax_throwsException() {
        UpdateUserProfilePreferencesRequest req = new UpdateUserProfilePreferencesRequest(
                null, "SWE", "City", BigDecimal.valueOf(100.00), BigDecimal.valueOf(50.00), PreferredWorkMode.ANY
        );

        assertThatThrownBy(() -> userService.updatePreferences(1L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot exceed target maximum salary");
    }

    @Test
    @DisplayName("Update user profile with blank fullName throws IllegalArgumentException")
    void updateProfile_blankFullName_throwsException() {
        assertThatThrownBy(() -> userService.updateProfile(1L, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or blank");
    }

    @Test
    @DisplayName("Update user profile with oversized fullName (>100 chars) throws IllegalArgumentException")
    void updateProfile_oversizedFullName_throwsException() {
        String longName = "A".repeat(101);
        assertThatThrownBy(() -> userService.updateProfile(1L, longName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot exceed 100 characters");
    }

    @Test
    @DisplayName("Update user profile for non-existent ID throws ResourceNotFoundException")
    void updateProfile_userNotFound_throwsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateProfile(999L, "New Name"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }
}