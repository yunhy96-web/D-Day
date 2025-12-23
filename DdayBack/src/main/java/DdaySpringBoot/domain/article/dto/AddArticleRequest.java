package DdaySpringBoot.domain.article.dto;

import DdaySpringBoot.domain.article.domain.Article;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class AddArticleRequest {

    @NotBlank(message = "주제는 필수입니다")
    private String topic;

    private String articleType = "NORMAL";

    @NotBlank(message = "제목은 필수입니다")
    private String title;

    @NotBlank(message = "내용은 필수입니다")
    private String content;

    public Article toEntity(Long createdBy) {
        return Article.builder()
                .topic(topic)
                .articleType(articleType)
                .title(title)
                .content(content)
                .createdBy(createdBy)
                .build();
    }
}
