package DdaySpringBoot.domain.article.application.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ArticleCreatedEvent {
    private final Long articleNo;
}
