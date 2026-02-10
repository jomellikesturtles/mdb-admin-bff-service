package com.mdb.adminbff;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CorsConfigTest {

    @Value("${cors.allowed-origins:}")
    private List<String> allowedOrigins;

    @Test
    void testCorsAllowedOriginsInjection() {
        System.out.println("DEBUG: allowedOrigins = " + allowedOrigins);
        assertThat(allowedOrigins).isNotNull();
        assertThat(allowedOrigins).contains("http://localhost:5173", "http://localhost:3000");
    }
}
