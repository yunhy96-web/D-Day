package DdaySpringBoot.domain.permission.presentation;

import DdaySpringBoot.domain.permission.application.PermissionService;
import DdaySpringBoot.domain.permission.dto.PermissionResponse;
import DdaySpringBoot.domain.user.domain.User;
import DdaySpringBoot.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Permission", description = "권한 API")
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @Operation(summary = "내 권한으로 접근 가능한 게시글 타입 조회")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getMyPermissions(
            @AuthenticationPrincipal User user) {
        List<PermissionResponse> permissions = permissionService.getPermissionsByRole(user.getRole().name())
                .stream()
                .map(PermissionResponse::new)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(permissions));
    }

    @Operation(summary = "내가 읽을 수 있는 게시글 타입 목록")
    @GetMapping("/my/readable")
    public ResponseEntity<ApiResponse<List<String>>> getMyReadableTypes(
            @AuthenticationPrincipal User user) {
        List<String> types = permissionService.getReadableArticleTypes(user.getRole().name());
        return ResponseEntity.ok(ApiResponse.success(types));
    }

    @Operation(summary = "내가 작성할 수 있는 게시글 타입 목록")
    @GetMapping("/my/writable")
    public ResponseEntity<ApiResponse<List<String>>> getMyWritableTypes(
            @AuthenticationPrincipal User user) {
        List<String> types = permissionService.getWritableArticleTypes(user.getRole().name());
        return ResponseEntity.ok(ApiResponse.success(types));
    }
}
