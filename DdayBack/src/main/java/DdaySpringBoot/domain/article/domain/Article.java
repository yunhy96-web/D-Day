package DdaySpringBoot.domain.article.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Table(name = "articles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "no", updatable = false)
    private Long no;

    @Column(name = "uuid", nullable = false, unique = true, updatable = false)
    private String uuid;

    @Column(name = "topic", length = 50)
    private String topic;

    @Column(name = "article_type", length = 20)
    private String articleType = "NORMAL";

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "original_lang", length = 10)
    private String originalLang;

    @Column(name = "title_ko")
    private String titleKo;

    @Column(name = "content_ko", columnDefinition = "TEXT")
    private String contentKo;

    @Column(name = "title_th")
    private String titleTh;

    @Column(name = "content_th", columnDefinition = "TEXT")
    private String contentTh;

    @Column(name = "translation_status", length = 20)
    private String translationStatus = "PENDING";

    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public Article(String topic, String articleType, String title, String content, Long createdBy) {
        this.uuid = UUID.randomUUID().toString();
        this.topic = topic;
        this.articleType = articleType != null ? articleType : "NORMAL";
        this.title = title;
        this.content = content;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
    }

    public void update(String topic, String articleType, String title, String content, Long updatedBy) {
        this.topic = topic;
        this.articleType = articleType;
        this.title = title;
        this.content = content;
        this.updatedBy = updatedBy;
        this.translationStatus = "PENDING";
        this.titleKo = null;
        this.contentKo = null;
        this.titleTh = null;
        this.contentTh = null;
        this.originalLang = null;
    }

    public void updateTranslation(String originalLang, String titleKo, String contentKo,
                                   String titleTh, String contentTh) {
        this.originalLang = originalLang;
        this.titleKo = titleKo;
        this.contentKo = contentKo;
        this.titleTh = titleTh;
        this.contentTh = contentTh;
        this.translationStatus = "COMPLETED";
    }

    public void setTranslationStatus(String status) {
        this.translationStatus = status;
    }

    public boolean isSecret() {
        return "SECRET".equals(this.articleType);
    }
}
