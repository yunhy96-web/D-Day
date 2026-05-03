package com.hauly.platform.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hauly.platform.auth.domain.model.AppUser;
import com.hauly.platform.auth.domain.model.Role;
import com.hauly.platform.auth.domain.model.Username;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test using the local 'hauly' Docker Postgres.
 * Uses @Transactional rollback to avoid polluting the DB between tests.
 * Note: unique test usernames avoid conflicts with any bootstrapped users.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class AuthControllerIT {

    private static final String TEST_USERNAME = "it-test-user";
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
        if (userRepository.findByUsername(Username.of(TEST_USERNAME)).isEmpty()) {
            AppUser user = AppUser.create(
                    Username.of(TEST_USERNAME),
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
        Map<String, String> body = Map.of("username", TEST_USERNAME, "password", TEST_PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value(TEST_USERNAME))
                .andExpect(jsonPath("$.user.role").value("INTAKE"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        Map<String, String> body = Map.of("username", TEST_USERNAME, "password", "wrong-password-12345");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withValidToken_returns200() throws Exception {
        Map<String, String> loginBody = Map.of("username", TEST_USERNAME, "password", TEST_PASSWORD);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson = loginResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseJson).get("accessToken").asText();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(TEST_USERNAME))
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
        Map<String, String> body = Map.of("username", TEST_USERNAME, "password", "short");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }
}
