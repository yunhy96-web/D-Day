package com.hauly.platform.i18n.presentation.rest;

import com.hauly.shared.kernel.Lang;
import com.hauly.platform.i18n.application.I18nMessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Public (permit-all) i18n endpoint — frontend needs messages before login.
 */
@RestController
@RequestMapping("/api/i18n")
public class I18nPublicController {

    private final I18nMessageService i18nMessageService;

    public I18nPublicController(I18nMessageService i18nMessageService) {
        this.i18nMessageService = i18nMessageService;
    }

    @GetMapping("/messages")
    public ResponseEntity<Map<String, String>> getMessages(
            @RequestParam(defaultValue = "ko") String lang,
            @RequestParam(required = false) String context) {
        Lang langEnum = Lang.fromString(lang);
        Map<String, String> messages = context != null
                ? i18nMessageService.getMessages(langEnum, context)
                : i18nMessageService.getMessages(langEnum);
        return ResponseEntity.ok(messages);
    }
}
