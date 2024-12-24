package DdaySpringBoot.controller;

import DdaySpringBoot.domain.Article;
import DdaySpringBoot.dto.AddArticleRequest;
import DdaySpringBoot.dto.ArticleResponse;
import DdaySpringBoot.dto.UpdateArticleRequest;
import DdaySpringBoot.service.BlogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
public class BlogApiController {

    private final BlogService blogService;

    //글 등록 //커밋 테스트용
    @PostMapping("/api/articles")
    public ResponseEntity<Article> addArticle(@RequestBody AddArticleRequest request){
        Article savedArticle = blogService.save(request);//커밋용
        int a = 0;
        return ResponseEntity.status(HttpStatus.CREATED).body(savedArticle);
    }
    //전체 조회 여기에 id도 출력해줘야 나중에 클릭시 id 조회 가능.
    @GetMapping("/api/articles")
    public ResponseEntity<List<ArticleResponse>> findAllArticles(){
        List<ArticleResponse> articles = blogService.findAll()
                .stream()
                .map(ArticleResponse::new)
                .toList();
        return ResponseEntity.ok()
                .body(articles);
    }
    //id 기반으로 하나 조회 test
    @GetMapping("/api/articles/{id}")
    public ResponseEntity<Article> findArticle(@PathVariable long id){
        try {
            Article article = blogService.findById(id);
            return ResponseEntity.ok().body(article);
        } catch (IllegalArgumentException e) {
            // 클라이언트가 존재하지 않는 ID를 전송한 경우
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    //글삭제
    @DeleteMapping("/api/articles/{id}")
    public ResponseEntity<Void> deleteArticle(@PathVariable long id){
        blogService.delete(id);
        return ResponseEntity.ok()
                .build();
    }
    //굴 수정
    @PutMapping("/api/articles/{id}")
    public ResponseEntity<Article> updateArticle(@PathVariable long id,
        @RequestBody UpdateArticleRequest request){
        Article updatedArticle = blogService.update(id, request);

        return ResponseEntity.ok()
                .body(updatedArticle);
    }




}
