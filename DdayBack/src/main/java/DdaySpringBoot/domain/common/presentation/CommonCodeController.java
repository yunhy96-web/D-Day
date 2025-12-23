package DdaySpringBoot.domain.common.presentation;

import DdaySpringBoot.domain.common.application.CommonCodeService;
import DdaySpringBoot.domain.common.dto.CommonCodeResponse;
import DdaySpringBoot.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "CommonCode", description = "공통코드 API")
@RestController
@RequestMapping("/api/codes")
@RequiredArgsConstructor
public class CommonCodeController {

    private final CommonCodeService commonCodeService;

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

    @Operation(summary = "게시글 타입 목록 조회 (일반글/비밀글)")
    @GetMapping("/article-types")
    public ResponseEntity<ApiResponse<List<CommonCodeResponse>>> getArticleTypes(
            @RequestParam(defaultValue = "en") String lang) {
        List<CommonCodeResponse> codes = commonCodeService.getCodesByGroup("ARTICLE_TYPE")
                .stream()
                .map(code -> new CommonCodeResponse(code, lang))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(codes));
    }
}
