package com.hauly.platform.commoncode.presentation.rest;

import com.hauly.platform.commoncode.application.CommonCodeService;
import com.hauly.platform.commoncode.application.command.CreateCodeCommand;
import com.hauly.platform.commoncode.application.command.UpdateCodeCommand;
import com.hauly.platform.commoncode.application.query.CommonCodeGroupView;
import com.hauly.platform.commoncode.application.query.CommonCodeView;
import com.hauly.platform.commoncode.presentation.dto.CreateCodeRequest;
import com.hauly.platform.commoncode.presentation.dto.UpdateCodeRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin endpoint for managing common code groups and codes.
 * Requires ADMIN role.
 */
@RestController
@RequestMapping("/api/admin/common-code-groups")
@PreAuthorize("hasRole('ADMIN')")
public class CommonCodeAdminController {

    private final CommonCodeService commonCodeService;

    public CommonCodeAdminController(CommonCodeService commonCodeService) {
        this.commonCodeService = commonCodeService;
    }

    @GetMapping
    public ResponseEntity<List<CommonCodeGroupView>> listGroups() {
        return ResponseEntity.ok(commonCodeService.listGroups());
    }

    @GetMapping("/{groupCode}/codes")
    public ResponseEntity<List<CommonCodeView>> listAllCodes(@PathVariable String groupCode) {
        return ResponseEntity.ok(commonCodeService.listAllCodesByGroup(groupCode));
    }

    @PostMapping("/{groupCode}/codes")
    public ResponseEntity<CommonCodeView> createCode(
            @PathVariable String groupCode,
            @Valid @RequestBody CreateCodeRequest req) {
        CreateCodeCommand command = new CreateCodeCommand(
                groupCode, req.code(), req.nameKo(), req.nameEn(), req.nameTh(), req.sortOrder());
        CommonCodeView view = commonCodeService.createCode(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(view);
    }

    @PatchMapping("/{groupCode}/codes/{code}")
    public ResponseEntity<CommonCodeView> updateCode(
            @PathVariable String groupCode,
            @PathVariable String code,
            @Valid @RequestBody UpdateCodeRequest req) {
        UpdateCodeCommand command = new UpdateCodeCommand(
                groupCode, code, req.nameKo(), req.nameEn(), req.nameTh(), req.sortOrder(), req.active());
        return ResponseEntity.ok(commonCodeService.updateCode(command));
    }

    @DeleteMapping("/{groupCode}/codes/{code}")
    public ResponseEntity<Void> deleteCode(
            @PathVariable String groupCode,
            @PathVariable String code) {
        commonCodeService.deleteCode(groupCode, code);
        return ResponseEntity.noContent().build();
    }
}
