package DdaySpringBoot.controller;

import DdaySpringBoot.common.response.ApiResponse;
import DdaySpringBoot.domain.Article;
import DdaySpringBoot.domain.User;
import DdaySpringBoot.dto.AddArticleRequest;
import DdaySpringBoot.dto.ArticleResponse;
import DdaySpringBoot.dto.UpdateArticleRequest;
import DdaySpringBoot.service.ArticleServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Article", description = "게시글 API")
@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleServiceInterface articleService;

    @Operation(summary = "게시글 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<ArticleResponse>> addArticle(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AddArticleRequest request) {
        Article savedArticle = articleService.save(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("게시글 등록 성공", new ArticleResponse(savedArticle, user.getTimezone())));
    }

    @Operation(summary = "전체 게시글 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ArticleResponse>>> findAllArticles(
            @AuthenticationPrincipal User user) {
        List<ArticleResponse> articles = articleService.findAll()
                .stream()
                .map(article -> new ArticleResponse(article, user.getTimezone()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(articles));
    }

    @Operation(summary = "게시글 단건 조회")
    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<ArticleResponse>> findArticle(
            @AuthenticationPrincipal User user,
            @PathVariable String uuid) {
        Article article = articleService.findByUuid(uuid);
        return ResponseEntity.ok(ApiResponse.success(new ArticleResponse(article, user.getTimezone())));
    }

    @Operation(summary = "게시글 삭제")
    @DeleteMapping("/{uuid}")
    public ResponseEntity<ApiResponse<Void>> deleteArticle(@PathVariable String uuid) {
        articleService.delete(uuid);
        return ResponseEntity.ok(ApiResponse.success("게시글 삭제 성공"));
    }

    @Operation(summary = "게시글 수정")
    @PutMapping("/{uuid}")
    public ResponseEntity<ApiResponse<ArticleResponse>> updateArticle(
            @AuthenticationPrincipal User user,
            @PathVariable String uuid,
            @Valid @RequestBody UpdateArticleRequest request) {
        Article updatedArticle = articleService.update(uuid, request);
        return ResponseEntity.ok(ApiResponse.success("게시글 수정 성공", new ArticleResponse(updatedArticle, user.getTimezone())));
    }
}
