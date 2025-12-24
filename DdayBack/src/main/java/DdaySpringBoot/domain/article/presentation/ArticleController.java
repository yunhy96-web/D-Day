package DdaySpringBoot.domain.article.presentation;

import DdaySpringBoot.domain.article.application.ArticleServiceInterface;
import DdaySpringBoot.domain.article.domain.Article;
import DdaySpringBoot.domain.article.dto.AddArticleRequest;
import DdaySpringBoot.domain.article.dto.ArticleResponse;
import DdaySpringBoot.domain.article.dto.UpdateArticleRequest;
import DdaySpringBoot.domain.permission.application.PermissionService;
import DdaySpringBoot.domain.user.domain.User;
import DdaySpringBoot.domain.user.domain.UserRepository;
import DdaySpringBoot.global.exception.AuthException;
import DdaySpringBoot.global.exception.ErrorCode;
import DdaySpringBoot.global.response.ApiResponse;
import DdaySpringBoot.global.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Tag(name = "Article", description = "게시글 API")
@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleServiceInterface articleService;
    private final UserRepository userRepository;
    private final PermissionService permissionService;

    @Operation(summary = "게시글 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<ArticleResponse>> addArticle(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AddArticleRequest request) {
        Article savedArticle = articleService.save(request, user.getNo(), user.getRole().name());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("게시글 등록 성공", new ArticleResponse(savedArticle, user.getNickname(), user.getTimezone())));
    }

    @Operation(summary = "전체 게시글 조회 (타입/주제별 필터링, 키워드 검색, 페이지네이션 지원)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ArticleResponse>>> findAllArticles(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String articleType,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String userRole = user.getRole().name();

        // 페이지 크기 제한 (최대 50)
        size = Math.min(size, 50);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Article> articlePage;

        // 키워드 검색이 있는 경우
        if (keyword != null && !keyword.trim().isEmpty()) {
            // articleType 권한 체크
            if (articleType != null && !articleType.isEmpty() && !permissionService.canRead(userRole, articleType)) {
                throw new AuthException(ErrorCode.ACCESS_DENIED, "해당 게시글 타입에 대한 접근 권한이 없습니다");
            }
            articlePage = articleService.searchByKeyword(userRole, articleType, topic, keyword.trim(), pageable);
        }
        // articleType이 지정되었고 해당 타입에 대한 읽기 권한이 있는 경우만 필터링
        else if (articleType != null && !articleType.isEmpty()) {
            if (!permissionService.canRead(userRole, articleType)) {
                throw new AuthException(ErrorCode.ACCESS_DENIED, "해당 게시글 타입에 대한 접근 권한이 없습니다");
            }
            if (topic != null && !topic.isEmpty()) {
                articlePage = articleService.findByArticleTypeAndTopicPaged(articleType, topic, pageable);
            } else {
                articlePage = articleService.findByArticleTypePaged(articleType, pageable);
            }
        } else if (topic != null && !topic.isEmpty()) {
            articlePage = articleService.findByTopicAndRolePaged(topic, userRole, pageable);
        } else {
            articlePage = articleService.findAllByRolePaged(userRole, pageable);
        }

        // 작성자 ID 목록 추출
        Set<Long> authorIds = articlePage.getContent().stream()
                .map(Article::getCreatedBy)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        // 작성자 정보 조회 및 Map 생성
        Map<Long, String> authorNicknameMap = userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(User::getNo, User::getNickname));

        List<ArticleResponse> responses = articlePage.getContent().stream()
                .map(article -> {
                    String nickname = article.getCreatedBy() != null
                            ? authorNicknameMap.getOrDefault(article.getCreatedBy(), "Unknown")
                            : "Anonymous";
                    return new ArticleResponse(article, nickname, user.getTimezone());
                })
                .toList();

        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(articlePage, responses)));
    }

    @Operation(summary = "주제별 게시글 조회 (권한에 따라 접근 가능한 타입만 조회)")
    @GetMapping("/topic/{topic}")
    public ResponseEntity<ApiResponse<List<ArticleResponse>>> findArticlesByTopic(
            @AuthenticationPrincipal User user,
            @PathVariable String topic) {
        List<Article> articles = articleService.findByTopicAndRole(topic, user.getRole().name());

        Set<Long> authorIds = articles.stream()
                .map(Article::getCreatedBy)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Map<Long, String> authorNicknameMap = userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(User::getNo, User::getNickname));

        List<ArticleResponse> responses = articles.stream()
                .map(article -> {
                    String nickname = article.getCreatedBy() != null
                            ? authorNicknameMap.getOrDefault(article.getCreatedBy(), "Unknown")
                            : "Anonymous";
                    return new ArticleResponse(article, nickname, user.getTimezone());
                })
                .toList();

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @Operation(summary = "게시글 단건 조회")
    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<ArticleResponse>> findArticle(
            @AuthenticationPrincipal User user,
            @PathVariable String uuid) {
        Article article = articleService.findByUuid(uuid);

        // 해당 게시글 타입에 대한 읽기 권한 체크
        if (!permissionService.canRead(user.getRole().name(), article.getArticleType())) {
            throw new AuthException(ErrorCode.ACCESS_DENIED, "해당 게시글에 대한 접근 권한이 없습니다");
        }

        String nickname = "Anonymous";
        if (article.getCreatedBy() != null) {
            nickname = userRepository.findById(article.getCreatedBy())
                    .map(User::getNickname)
                    .orElse("Unknown");
        }
        return ResponseEntity.ok(ApiResponse.success(new ArticleResponse(article, nickname, user.getTimezone())));
    }

    @Operation(summary = "게시글 삭제")
    @DeleteMapping("/{uuid}")
    public ResponseEntity<ApiResponse<Void>> deleteArticle(
            @AuthenticationPrincipal User user,
            @PathVariable String uuid) {
        articleService.delete(uuid, user.getNo(), user.getRole().name());
        return ResponseEntity.ok(ApiResponse.success("게시글 삭제 성공"));
    }

    @Operation(summary = "게시글 수정")
    @PutMapping("/{uuid}")
    public ResponseEntity<ApiResponse<ArticleResponse>> updateArticle(
            @AuthenticationPrincipal User user,
            @PathVariable String uuid,
            @Valid @RequestBody UpdateArticleRequest request) {
        Article updatedArticle = articleService.update(uuid, request, user.getNo(), user.getRole().name());
        return ResponseEntity.ok(ApiResponse.success("게시글 수정 성공", new ArticleResponse(updatedArticle, user.getNickname(), user.getTimezone())));
    }
}
