package DdaySpringBoot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
//git 추가용
public class UpdateArticleRequest {
    private String title;
    private String content;
}
