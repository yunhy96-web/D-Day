package com.hauly.platform.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hauly.platform.auth.domain.model.AppUser;
import com.hauly.platform.auth.domain.model.Email;
import com.hauly.platform.auth.domain.model.Role;
import com.hauly.platform.auth.domain.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test using the local 'hauly' Docker Postgres.
 * Uses @Transactional rollback to avoid polluting the DB between tests.
 * Note: unique test emails avoid conflicts with any bootstrapped users.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class AuthControllerIT {

    private static final String TEST_EMAIL    = "it-test@hauly.local";
    private static final String TEST_PASSWORD = "changeme-12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Create a test user inline (BCrypt cost 4 via test profile if configured, falls back to 12)
        if (userRepository.findByEmail(Email.of(TEST_EMAIL)).isEmpty()) {
            AppUser user = AppUser.create(
                    Email.of(TEST_EMAIL),
                    TEST_PASSWORD,
                    Role.INTAKE,
                    "IT Test User",
                    rawPwd -> passwordEncoder.encode(rawPwd)
            );
            userRepository.save(user);
        }
    }

    @Test
    void health_returnsOk() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void me_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_success_returns200WithTokens() throws Exception {
        Map<String, String> body = Map.of("email", TEST_EMAIL, "password", TEST_PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.user.role").value("INTAKE"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        Map<String, String> body = Map.of("email", TEST_EMAIL, "password", "wrong-password-12345");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withValidToken_returns200() throws Exception {
        // Login to get token
        Map<String, String> loginBody = Map.of("email", TEST_EMAIL, "password", TEST_PASSWORD);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson = loginResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseJson).get("accessToken").asText();

        // Use token to access /me
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.role").value("INTAKE"));
    }

    @Test
    void me_withMalformedJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer not-a-valid-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_shortPassword_returns400() throws Exception {
        Map<String, String> body = Map.of("email", TEST_EMAIL, "password", "short");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }
}
