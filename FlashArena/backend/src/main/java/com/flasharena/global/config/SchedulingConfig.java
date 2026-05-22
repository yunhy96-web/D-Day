package com.flasharena.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @Scheduled 활성화. OutboxRelayer 가 주기적으로 READY 이벤트를 Redis Stream 으로 발행하기 위함.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
