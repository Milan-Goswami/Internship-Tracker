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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    public UserService(UserRepository userRepository, UserProfileRepository userProfileRepository) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
    }

    /**
     * Normalizes an email address by trimming whitespace and converting to lowercase.
     */
    public String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    /**
     * Looks up a user by email address (case-insensitive via normalization).
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByEmail(normalizedEmail);
    }

    /**
     * Looks up a user by primary key ID.
     */
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return userRepository.findById(id);
    }

    /**
     * Checks if a user already exists with the given email address.
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            return false;
        }
        return userRepository.existsByEmail(normalizedEmail);
    }

    /**
     * Creates and persists a new user account with normalized email and domain validation.
     * Note: passwordHash must be pre-hashed by the security/auth layer.
     */
    @Transactional
    public User createUser(String email, String passwordHash, String fullName, UserRole role) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash cannot be null or blank");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name cannot be null or blank");
        }

        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new UserAlreadyExistsException("User with email '" + normalizedEmail + "' already exists");
        }

        UserRole effectiveRole = role != null ? role : UserRole.ROLE_USER;
        User user = new User(normalizedEmail, passwordHash, fullName.trim(), effectiveRole);

        return userRepository.save(user);
    }

    /**
     * Fetches current user profile and target preferences.
     * Works seamlessly even when no UserProfile row has been created yet.
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        UserProfile profile = userProfileRepository.findById(userId).orElse(null);
        return UserProfileResponse.from(user, profile);
    }

    /**
     * Updates an existing user's mutable profile fields (fullName) for PATCH.
     */
    @Transactional
    public UserProfileResponse updateProfile(Long userId, String fullName) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name cannot be null or blank");
        }
        String trimmedName = fullName.trim();
        if (trimmedName.length() > 100) {
            throw new IllegalArgumentException("Full name cannot exceed 100 characters");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setFullName(trimmedName);
        User savedUser = userRepository.save(user);

        UserProfile profile = userProfileRepository.findById(userId).orElse(null);
        return UserProfileResponse.from(savedUser, profile);
    }

    /**
     * Creates or updates target preferences and candidate profile for PUT.
     */
    @Transactional
    public UserProfileResponse updatePreferences(Long userId, UpdateUserProfilePreferencesRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        request.validateSalaryRange();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        User targetUser = user;
        if (request.fullName() != null && !request.fullName().isBlank()) {
            String trimmedName = request.fullName().trim();
            if (trimmedName.length() > 100) {
                throw new IllegalArgumentException("Full name cannot exceed 100 characters");
            }
            targetUser.setFullName(trimmedName);
            targetUser = userRepository.save(targetUser);
        }

        final User finalUser = targetUser;
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseGet(() -> new UserProfile(finalUser));

        profile.setTargetTitle(request.targetTitle() != null ? request.targetTitle().trim() : null);
        profile.setTargetLocation(request.targetLocation() != null ? request.targetLocation().trim() : null);
        profile.setTargetSalaryMin(request.targetSalaryMin());
        profile.setTargetSalaryMax(request.targetSalaryMax());
        profile.setPreferredWorkMode(request.preferredWorkMode() != null ? request.preferredWorkMode() : PreferredWorkMode.ANY);

        UserProfile savedProfile = userProfileRepository.save(profile);

        return UserProfileResponse.from(finalUser, savedProfile);
    }
}