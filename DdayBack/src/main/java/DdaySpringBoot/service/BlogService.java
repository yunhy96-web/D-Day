package DdaySpringBoot.service;

import DdaySpringBoot.domain.Article;
import DdaySpringBoot.dto.AddArticleRequest;
import DdaySpringBoot.repository.BlogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service //빈으로 등록
public class BlogService {
    private final BlogRepository blogRepository;

    //블로그 글 추가하는 메서드
    public Article save(AddArticleRequest request){
        return blogRepository.save(request.toEntity());
    }

}
