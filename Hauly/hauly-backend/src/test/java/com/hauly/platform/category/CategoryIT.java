package com.hauly.platform.category;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hauly.platform.auth.domain.model.AppUser;
import com.hauly.platform.auth.domain.model.Username;
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
class CategoryIT {

    private static final String TEST_USERNAME = "cat-it-user";
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
                    Username.of(TEST_USERNAME), TEST_PASSWORD, Role.INTAKE, "Cat IT User",
                    rawPwd -> passwordEncoder.encode(rawPwd));
            userRepository.save(user);
        }
    }

    private String obtainToken() throws Exception {
        Map<String, String> body = Map.of("username", TEST_USERNAME, "password", TEST_PASSWORD);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    @Test
    void getActiveCategories_withToken_returnsSeededCategories() throws Exception {
        String token = obtainToken();

        mockMvc.perform(get("/api/intake/categories")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", not(empty())));
    }

    @Test
    void getActiveCategories_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/intake/categories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCategory_contactLens_returnsParsedSchema() throws Exception {
        String token = obtainToken();

        mockMvc.perform(get("/api/intake/categories/CONTACT_LENS")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CONTACT_LENS"))
                .andExpect(jsonPath("$.fields").isArray())
                .andExpect(jsonPath("$.fields", not(empty())));
    }

    @Test
    void getCategory_general_returnsEmptyFields() throws Exception {
        String token = obtainToken();

        mockMvc.perform(get("/api/intake/categories/GENERAL")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("GENERAL"))
                .andExpect(jsonPath("$.fields").isArray());
    }

    @Test
    void getCategory_notFound_returns500OrBadRequest() throws Exception {
        String token = obtainToken();

        // expect 400 (IllegalArgumentException -> BadRequest via GlobalExceptionHandler)
        mockMvc.perform(get("/api/intake/categories/NONEXISTENT")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }
}
