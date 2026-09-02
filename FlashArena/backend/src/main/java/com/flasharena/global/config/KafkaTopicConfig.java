package com.flasharena.global.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** 주문 이벤트 토픽. 앱 기동 시 KafkaAdmin 이 없으면 만든다(있으면 그대로). */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic orderEventsTopic(@Value("${app.kafka.topic:flasharena.order-events}") String topic,
            @Value("${app.kafka.partitions:3}") int partitions) {
        return TopicBuilder.name(topic).partitions(partitions).replicas(1).build();
    }
}
