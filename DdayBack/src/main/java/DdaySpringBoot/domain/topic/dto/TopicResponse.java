package DdaySpringBoot.domain.topic.dto;

import DdaySpringBoot.domain.topic.domain.Topic;
import lombok.Getter;

@Getter
public class TopicResponse {

    private final Long no;
    private final String code;
    private final String name;
    private final String nameKo;
    private final String nameTh;
    private final String icon;
    private final String color;
    private final Integer sortOrder;

    public TopicResponse(Topic topic) {
        this.no = topic.getNo();
        this.code = topic.getCode();
        this.name = topic.getName();
        this.nameKo = topic.getNameKo();
        this.nameTh = topic.getNameTh();
        this.icon = topic.getIcon();
        this.color = topic.getColor();
        this.sortOrder = topic.getSortOrder();
    }
}
