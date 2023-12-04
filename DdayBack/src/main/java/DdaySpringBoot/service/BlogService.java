package DdaySpringBoot.service;

import DdaySpringBoot.domain.Article;
import DdaySpringBoot.dto.AddArticleRequest;
import DdaySpringBoot.dto.UpdateArticleRequest;
import DdaySpringBoot.repository.BlogRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service //빈으로 등록
public class BlogService {
    private final BlogRepository blogRepository;

    //블로그 글 추가하는 메서드
    public Article save(AddArticleRequest request){
        return blogRepository.save(request.toEntity());
    }
    //전체 조회
    public List<Article> findAll(){
        return blogRepository.findAll();
    }

    //특정 글 조회
    public Article findById(long id){
        return blogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("not found : " + id));
    }
    //글삭제
    public void delete(long id){
        blogRepository.deleteById(id);
    }
    //글 수정
    @Transactional
    public Article update(long id, UpdateArticleRequest request){
        Article article = blogRepository.findById(id)
                .orElseThrow(()->new IllegalArgumentException("not found : " + id));

        article.update(request.getTitle(), request.getContent());

        return article;
    }

}
