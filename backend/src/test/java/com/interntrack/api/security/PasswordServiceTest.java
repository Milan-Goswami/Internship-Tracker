package com.interntrack.api.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordServiceTest {

    private PasswordService passwordService;

    @BeforeEach
    void setUp() {
        passwordService = new BcryptPasswordService();
    }

    @Test
    @DisplayName("Should successfully hash raw password using BCrypt with salt")
    void hash_whenValidPassword_producesBcryptHash() {
        String raw = "StrongP@ssw0rd123";
        String hash = passwordService.hash(raw);

        assertThat(hash).isNotNull();
        assertThat(hash).startsWith("$").hasSize(60);
        assertThat(hash).isNotEqualTo(raw);
    }

    @Test
    @DisplayName("Should correctly verify raw password against valid hash")
    void matches_whenCorrectPassword_returnsTrue() {
        String raw = "CandidateSecure#2026";
        String hash = passwordService.hash(raw);

        boolean matches = passwordService.matches(raw, hash);

        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("Should reject incorrect raw password against valid hash")
    void matches_whenIncorrectPassword_returnsFalse() {
        String raw = "CandidateSecure#2026";
        String wrong = "CandidateWrong#2026";
        String hash = passwordService.hash(raw);

        boolean matches = passwordService.matches(wrong, hash);

        assertThat(matches).isFalse();
    }

    @Test
    @DisplayName("Should generate different salt and hashes for repeated hashing of identical password")
    void hash_repeatedCalls_produceDifferentHashesDueToSalting() {
        String raw = "DeterministicInput@123";

        String hash1 = passwordService.hash(raw);
        String hash2 = passwordService.hash(raw);

        assertThat(hash1).isNotEqualTo(hash2);
        assertThat(passwordService.matches(raw, hash1)).isTrue();
        assertThat(passwordService.matches(raw, hash2)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Should reject null or blank passwords with IllegalArgumentException")
    void hash_whenBlankOrNull_throwsException(String invalidPassword) {
        assertThatThrownBy(() -> passwordService.hash(invalidPassword))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password must not be null or blank");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567", "short", "abc", "a"})
    @DisplayName("Should reject passwords shorter than 8 characters")
    void hash_whenShorterThanMin_throwsException(String shortPassword) {
        assertThatThrownBy(() -> passwordService.hash(shortPassword))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 8 and 100 characters");
    }

    @Test
    @DisplayName("Should reject passwords longer than 100 characters")
    void hash_whenLongerThanMax_throwsException() {
        String longPassword = "A".repeat(101);

        assertThatThrownBy(() -> passwordService.hash(longPassword))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 8 and 100 characters");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("matches should return false gracefully on null or blank raw password or hash")
    void matches_whenInvalidInput_returnsFalseGracefully(String invalidInput) {
        assertThat(passwordService.matches(invalidInput, "")).isFalse();
        assertThat(passwordService.matches("ValidPassword123", invalidInput)).isFalse();
        assertThat(passwordService.matches(null, null)).isFalse();
    }

    @Test
    @DisplayName("isValidPassword evaluates policy boundary accurately")
    void isValidPassword_boundaryChecks() {
        assertThat(passwordService.isValidPassword(null)).isFalse();
        assertThat(passwordService.isValidPassword("")).isFalse();
        assertThat(passwordService.isValidPassword("   ")).isFalse();
        assertThat(passwordService.isValidPassword("1234567")).isFalse(); // 7 chars
        assertThat(passwordService.isValidPassword("12345678")).isTrue(); // 8 chars (min)
        assertThat(passwordService.isValidPassword("A".repeat(100))).isTrue(); // 100 chars (max)
        assertThat(passwordService.isValidPassword("A".repeat(101))).isFalse(); // 101 chars
    }
}