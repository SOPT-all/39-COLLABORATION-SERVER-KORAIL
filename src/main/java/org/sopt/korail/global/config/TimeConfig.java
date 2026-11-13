package org.sopt.korail.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {
    // 서비스 시간 기준은 KST로 통일
    public static final ZoneId KST = ZoneId.of("Asia/Seoul");

    // 현재 시각은 LocalDateTime.now() 대신 LocalDateTime.now(clock)으로 구한다.
    @Bean
    public Clock clock() {
        return Clock.system(KST);
    }
}
