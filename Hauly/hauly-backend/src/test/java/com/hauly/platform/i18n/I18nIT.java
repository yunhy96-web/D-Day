package com.hauly.platform.i18n;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class I18nIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getMessages_ko_withoutAuth_returns200() throws Exception {
        mockMvc.perform(get("/api/i18n/messages?lang=ko"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isMap());
    }

    @Test
    void getMessages_koCommon_returnsSeededKeys() throws Exception {
        mockMvc.perform(get("/api/i18n/messages?lang=ko&context=common"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['btn.cancel']").value("취소"));
    }

    @Test
    void getMessages_th_returnsThaiMessages() throws Exception {
        mockMvc.perform(get("/api/i18n/messages?lang=th&context=common"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['btn.cancel']").isString());
    }

    @Test
    void getMessages_noContext_returnsAllMessages() throws Exception {
        mockMvc.perform(get("/api/i18n/messages?lang=ko"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isMap());
    }
}
