package DdaySpringBoot.domain.topic.presentation;

import DdaySpringBoot.domain.topic.domain.Topic;
import DdaySpringBoot.domain.topic.domain.TopicRepository;
import DdaySpringBoot.domain.topic.dto.TopicResponse;
import DdaySpringBoot.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Topic", description = "주제/카테고리 API")
@RestController
@RequestMapping("/api/topics")
@RequiredArgsConstructor
public class TopicController {

    private final TopicRepository topicRepository;

    @Operation(summary = "전체 주제 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<TopicResponse>>> findAllTopics() {
        List<TopicResponse> topics = topicRepository.findByIsActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(TopicResponse::new)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(topics));
    }
}
