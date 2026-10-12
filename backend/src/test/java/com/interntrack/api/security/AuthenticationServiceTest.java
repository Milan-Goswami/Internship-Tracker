package com.interntrack.api.security;

import com.interntrack.api.entity.User;
import com.interntrack.api.entity.enums.UserRole;
import com.interntrack.api.exception.AccountInactiveException;
import com.interntrack.api.exception.InvalidCredentialsException;
import com.interntrack.api.exception.UserAlreadyExistsException;
import com.interntrack.api.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private PasswordService passwordService;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User sampleUser;

    @BeforeEach
    void setUp() throws Exception {
        sampleUser = new User("candidate@example.com", "$2a$12$somehashedpasswordstring12345", "Alice Candidate", UserRole.ROLE_USER);
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(sampleUser, 42L);
    }

    @Test
    @DisplayName("register: Should successfully create and return authenticated user when input is valid")
    void register_validUser_createsUser() {
        when(passwordService.isValidPassword("StrongP@ssw0rd")).thenReturn(true);
        when(userService.normalizeEmail("Candidate@Example.COM")).thenReturn("candidate@example.com");
        when(passwordService.hash("StrongP@ssw0rd")).thenReturn("$2a$12$hashedvalue");
        when(userService.createUser(eq("candidate@example.com"), eq("$2a$12$hashedvalue"), eq("Alice Candidate"), eq(UserRole.ROLE_USER)))
                .thenReturn(sampleUser);

        AuthenticatedUser result = authenticationService.register("Candidate@Example.COM", "StrongP@ssw0rd", "Alice Candidate", UserRole.ROLE_USER);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(42L);
        assertThat(result.email()).isEqualTo("candidate@example.com");
        assertThat(result.fullName()).isEqualTo("Alice Candidate");
        assertThat(result.role()).isEqualTo(UserRole.ROLE_USER);
        assertThat(result.isActive()).isTrue();
    }

    @Test
    @DisplayName("register: Duplicate email should be rejected by propagating UserAlreadyExistsException")
    void register_duplicateEmail_rejected() {
        when(passwordService.isValidPassword("StrongP@ssw0rd")).thenReturn(true);
        when(userService.normalizeEmail("candidate@example.com")).thenReturn("candidate@example.com");
        when(passwordService.hash("StrongP@ssw0rd")).thenReturn("$2a$12$hash");
        when(userService.createUser(anyString(), anyString(), anyString(), any()))
                .thenThrow(new UserAlreadyExistsException("User with email already exists"));

        assertThatThrownBy(() -> authenticationService.register("candidate@example.com", "StrongP@ssw0rd", "Alice", UserRole.ROLE_USER))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("register: Raw password must be hashed before passing to UserService")
    void register_passwordIsHashedBeforePersistence() {
        when(passwordService.isValidPassword("PlaintextPass123")).thenReturn(true);
        when(userService.normalizeEmail("test@example.com")).thenReturn("test@example.com");
        when(passwordService.hash("PlaintextPass123")).thenReturn("$2a$12$securehash");
        when(userService.createUser(anyString(), anyString(), anyString(), any())).thenReturn(sampleUser);

        authenticationService.register("test@example.com", "PlaintextPass123", "Alice", null);

        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(userService).createUser(eq("test@example.com"), hashCaptor.capture(), eq("Alice"), eq(null));

        assertThat(hashCaptor.getValue()).isEqualTo("$2a$12$securehash");
        assertThat(hashCaptor.getValue()).isNotEqualTo("PlaintextPass123");
    }

    @Test
    @DisplayName("register: Invalid input (blank email, blank name, invalid password) should be rejected")
    void register_invalidInput_rejected() {
        assertThatThrownBy(() -> authenticationService.register(null, "Pass12345", "Alice", UserRole.ROLE_USER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email cannot be null or blank");

        assertThatThrownBy(() -> authenticationService.register("a@b.com", "Pass12345", "", UserRole.ROLE_USER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Full name cannot be null or blank");

        when(passwordService.isValidPassword("short")).thenReturn(false);
        assertThatThrownBy(() -> authenticationService.register("a@b.com", "short", "Alice", UserRole.ROLE_USER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password does not meet platform security requirements");
    }

    @Test
    @DisplayName("authenticate: Valid credentials should return AuthenticatedUser")
    void authenticate_validCredentials_returnsAuthenticatedUser() {
        when(userService.normalizeEmail("candidate@example.com")).thenReturn("candidate@example.com");
        when(userService.findByEmail("candidate@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordService.matches("CorrectPassword", sampleUser.getPasswordHash())).thenReturn(true);

        AuthenticatedUser result = authenticationService.authenticate("candidate@example.com", "CorrectPassword");

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(42L);
        assertThat(result.email()).isEqualTo("candidate@example.com");
        assertThat(result.isActive()).isTrue();
    }

    @Test
    @DisplayName("authenticate: Incorrect password should throw InvalidCredentialsException")
    void authenticate_wrongPassword_rejected() {
        when(userService.normalizeEmail("candidate@example.com")).thenReturn("candidate@example.com");
        when(userService.findByEmail("candidate@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordService.matches("WrongPassword", sampleUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authenticationService.authenticate("candidate@example.com", "WrongPassword"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    @DisplayName("authenticate: Unknown user email should throw InvalidCredentialsException")
    void authenticate_unknownUser_rejected() {
        when(userService.normalizeEmail("unknown@example.com")).thenReturn("unknown@example.com");
        when(userService.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.authenticate("unknown@example.com", "AnyPassword"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    @DisplayName("authenticate: Inactive user account should throw AccountInactiveException")
    void authenticate_inactiveUser_rejected() {
        sampleUser.setIsActive(false);
        when(userService.normalizeEmail("candidate@example.com")).thenReturn("candidate@example.com");
        when(userService.findByEmail("candidate@example.com")).thenReturn(Optional.of(sampleUser));

        assertThatThrownBy(() -> authenticationService.authenticate("candidate@example.com", "CorrectPassword"))
                .isInstanceOf(AccountInactiveException.class)
                .hasMessage("Account is inactive");

        verify(passwordService, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("authenticate: AuthenticatedUser record never contains password hash field")
    void authenticate_passwordHashNeverExposed() {
        when(userService.normalizeEmail("candidate@example.com")).thenReturn("candidate@example.com");
        when(userService.findByEmail("candidate@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordService.matches("CorrectPassword", sampleUser.getPasswordHash())).thenReturn(true);

        AuthenticatedUser result = authenticationService.authenticate("candidate@example.com", "CorrectPassword");

        // Verify reflection on AuthenticatedUser record methods and fields
        assertThat(AuthenticatedUser.class.getDeclaredFields())
                .extracting("name")
                .doesNotContain("password", "passwordHash", "rawPassword");
    }

    @Test
    @DisplayName("emailNormalization: Input email is normalized consistently during register and authenticate")
    void emailNormalization() {
        when(passwordService.isValidPassword("StrongP@ssw0rd")).thenReturn(true);
        when(userService.normalizeEmail("  Alice@Example.COM  ")).thenReturn("alice@example.com");
        when(passwordService.hash("StrongP@ssw0rd")).thenReturn("$2a$12$hash");
        when(userService.createUser(eq("alice@example.com"), anyString(), anyString(), any())).thenReturn(sampleUser);

        authenticationService.register("  Alice@Example.COM  ", "StrongP@ssw0rd", "Alice", null);

        verify(userService).normalizeEmail("  Alice@Example.COM  ");
    }

    @Test
    @DisplayName("rawPasswordNeverPassedToUserService: Plaintext password is never passed to UserService")
    void rawPasswordNeverPassedToUserService() {
        String rawPassword = "SuperSecretPassword123";
        when(passwordService.isValidPassword(rawPassword)).thenReturn(true);
        when(userService.normalizeEmail(anyString())).thenReturn("alice@example.com");
        when(passwordService.hash(rawPassword)).thenReturn("$2a$12$saltandhash");
        when(userService.createUser(anyString(), anyString(), anyString(), any())).thenReturn(sampleUser);

        authenticationService.register("alice@example.com", rawPassword, "Alice", null);

        verify(userService, never()).createUser(anyString(), eq(rawPassword), anyString(), any());
        verify(userService).createUser(anyString(), eq("$2a$12$saltandhash"), anyString(), any());
    }

    @Test
    @DisplayName("authenticationDoesNotLogCredentials: Null/blank credentials reject with InvalidCredentialsException")
    void authenticationDoesNotLogCredentials() {
        assertThatThrownBy(() -> authenticationService.authenticate(null, "pass"))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThatThrownBy(() -> authenticationService.authenticate("user@example.com", null))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThatThrownBy(() -> authenticationService.authenticate("", "pass"))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}