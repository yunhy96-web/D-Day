package DdaySpringBoot.domain.common.presentation;

import DdaySpringBoot.domain.common.application.CommonCodeService;
import DdaySpringBoot.domain.common.dto.CommonCodeResponse;
import DdaySpringBoot.domain.user.domain.User;
import DdaySpringBoot.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@Tag(name = "CommonCode", description = "공통코드 API")
@RestController
@RequestMapping("/api/codes")
@RequiredArgsConstructor
public class CommonCodeController {

    private final CommonCodeService commonCodeService;

    // 관리자 권한이 필요한 article type 코드들
    private static final Set<String> ADMIN_ONLY_ARTICLE_TYPES = Set.of("SECRET", "NOTICE");

    @Operation(summary = "그룹별 공통코드 조회")
    @GetMapping("/{groupCode}")
    public ResponseEntity<ApiResponse<List<CommonCodeResponse>>> getCodesByGroup(
            @PathVariable String groupCode,
            @RequestParam(defaultValue = "en") String lang) {
        List<CommonCodeResponse> codes = commonCodeService.getCodesByGroup(groupCode)
                .stream()
                .map(code -> new CommonCodeResponse(code, lang))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(codes));
    }

    @Operation(summary = "게시글 주제 목록 조회")
    @GetMapping("/article-topics")
    public ResponseEntity<ApiResponse<List<CommonCodeResponse>>> getArticleTopics(
            @RequestParam(defaultValue = "en") String lang) {
        List<CommonCodeResponse> codes = commonCodeService.getCodesByGroup("ARTICLE_TOPIC")
                .stream()
                .map(code -> new CommonCodeResponse(code, lang))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(codes));
    }

    @Operation(summary = "게시글 타입 목록 조회 (권한에 따라 필터링)")
    @GetMapping("/article-types")
    public ResponseEntity<ApiResponse<List<CommonCodeResponse>>> getArticleTypes(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "en") String lang) {

        boolean isAdmin = user != null &&
                ("DEV".equals(user.getRole().name()) || "ADMIN".equals(user.getRole().name()));

        List<CommonCodeResponse> codes = commonCodeService.getCodesByGroup("ARTICLE_TYPE")
                .stream()
                .filter(code -> isAdmin || !ADMIN_ONLY_ARTICLE_TYPES.contains(code.getCode()))
                .map(code -> new CommonCodeResponse(code, lang))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(codes));
    }
}
