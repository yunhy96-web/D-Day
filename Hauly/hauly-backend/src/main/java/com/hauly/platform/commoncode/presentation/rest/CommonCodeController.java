package com.hauly.platform.commoncode.presentation.rest;

import com.hauly.platform.commoncode.application.CommonCodeService;
import com.hauly.platform.commoncode.application.query.CommonCodeView;
import com.hauly.shared.kernel.Lang;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public-intake (authenticated) endpoint for reading active common codes.
 */
@RestController
@RequestMapping("/api/intake/common-codes")
public class CommonCodeController {

    private final CommonCodeService commonCodeService;

    public CommonCodeController(CommonCodeService commonCodeService) {
        this.commonCodeService = commonCodeService;
    }

    @GetMapping("/{groupCode}")
    public ResponseEntity<List<CommonCodeView>> getActiveCodes(
            @PathVariable String groupCode,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        Lang lang = Lang.fromAcceptLanguage(acceptLanguage);
        List<CommonCodeView> codes = commonCodeService.getActiveCodes(groupCode, lang);
        return ResponseEntity.ok(codes);
    }
}
