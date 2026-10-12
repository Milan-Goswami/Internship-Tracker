package com.interntrack.api.service;

import com.interntrack.api.entity.RefreshToken;
import com.interntrack.api.entity.User;
import com.interntrack.api.entity.enums.UserRole;
import com.interntrack.api.exception.InvalidTokenException;
import com.interntrack.api.repository.RefreshTokenRepository;
import com.interntrack.api.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "security.jwt.secret=CONCURRENCY_TEST_SECRET_0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"
})
class RefreshTokenConcurrencyIntegrationTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private String initialRawToken;

    @BeforeEach
    void setUp() {
        // Clean any existing tokens/users
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User("concurrency-test@example.com", "$2a$12$hash", "Concurrent Tester", UserRole.ROLE_USER);
        testUser.setIsActive(true);
        testUser = userRepository.save(testUser);

        initialRawToken = refreshTokenService.createRefreshToken(testUser);
    }

    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Concurrent refresh requests with same token: exactly 1 succeeds and other is rejected via pessimistic locking")
    void concurrentRefresh_withSameToken_exactlyOneSucceeds() throws Exception {
        int numberOfThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    refreshTokenService.rotateRefreshToken(initialRawToken);
                    successCount.incrementAndGet();
                } catch (InvalidTokenException e) {
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = finishLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);
    }
}