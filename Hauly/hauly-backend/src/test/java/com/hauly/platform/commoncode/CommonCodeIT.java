package com.hauly.platform.commoncode;

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

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class CommonCodeIT {

    private static final String TEST_EMAIL = "cc-it@hauly.local";
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
        if (userRepository.findByEmail(Email.of(TEST_EMAIL)).isEmpty()) {
            AppUser user = AppUser.create(
                    Email.of(TEST_EMAIL), TEST_PASSWORD, Role.INTAKE, "CC IT User",
                    rawPwd -> passwordEncoder.encode(rawPwd));
            userRepository.save(user);
        }
    }

    private String obtainToken() throws Exception {
        Map<String, String> body = Map.of("email", TEST_EMAIL, "password", TEST_PASSWORD);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    @Test
    void getActiveCodes_withToken_returns200WithSeededCodes() throws Exception {
        String token = obtainToken();

        mockMvc.perform(get("/api/intake/common-codes/FULFILLMENT_STATUS")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", not(empty())));
    }

    @Test
    void getActiveCodes_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/intake/common-codes/FULFILLMENT_STATUS"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpoint_withIntakeRole_returns403() throws Exception {
        String token = obtainToken();

        mockMvc.perform(get("/api/admin/common-code-groups")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
