package DdaySpringBoot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
//git 추가용 배포테스트4
public class UpdateArticleRequest {
    private String title;
    private String content;
}
