package com.hauly.platform.i18n.presentation.rest;

import com.hauly.platform.i18n.application.I18nMessageService;
import com.hauly.platform.i18n.application.command.UpsertMessageCommand;
import com.hauly.platform.i18n.application.query.I18nMessageRowView;
import com.hauly.platform.i18n.presentation.dto.UpsertMessageRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin endpoint for managing i18n messages.
 * Requires ADMIN role.
 */
@RestController
@RequestMapping("/api/admin/i18n")
@PreAuthorize("hasRole('ADMIN')")
public class I18nAdminController {

    private final I18nMessageService i18nMessageService;

    public I18nAdminController(I18nMessageService i18nMessageService) {
        this.i18nMessageService = i18nMessageService;
    }

    @GetMapping("/messages")
    public ResponseEntity<List<I18nMessageRowView>> listMessages(
            @RequestParam(required = false) String context) {
        return ResponseEntity.ok(i18nMessageService.listAllRows(context));
    }

    @PutMapping("/messages/{key}")
    public ResponseEntity<Void> upsertMessage(
            @PathVariable String key,
            @Valid @RequestBody UpsertMessageRequest req) {
        i18nMessageService.upsertMessage(new UpsertMessageCommand(
                key, req.ko(), req.en(), req.th(), req.context()));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/messages/{key}")
    public ResponseEntity<Void> deleteMessage(@PathVariable String key) {
        i18nMessageService.deleteKey(key);
        return ResponseEntity.noContent().build();
    }
}
