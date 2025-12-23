package DdaySpringBoot.domain.article.application.event;

import DdaySpringBoot.domain.article.application.TranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleEventListener {

    private final TranslationService translationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleArticleCreated(ArticleCreatedEvent event) {
        log.info("Article created event received for article: {}", event.getArticleNo());
        translationService.translateArticle(event.getArticleNo());
    }
}
