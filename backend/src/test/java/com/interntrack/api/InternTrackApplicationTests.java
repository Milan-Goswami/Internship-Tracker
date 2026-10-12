package com.interntrack.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "security.jwt.secret=TEST_ONLY_MOCK_KEY_0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"
})
class InternTrackApplicationTests {

    @Test
    void contextLoads() {
    }
}