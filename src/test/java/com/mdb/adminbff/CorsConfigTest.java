package com.mdb.adminbff;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CorsConfigTest {

    @Value("${cors.allowed-origins:}")
    private List<String> allowedOrigins;

    @Test
    void testCorsAllowedOriginsInjection() {
        System.out.println("DEBUG: allowedOrigins = " + allowedOrigins);
        assertThat(allowedOrigins).isNotNull();
        // Since we changed to * in SecurityConfig, we can either keep the original check for application.yml values
        // or check if it contains the expected development origins.
        assertThat(allowedOrigins).contains("http://localhost:5173");
    }
}
