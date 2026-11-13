package org.sopt.korail;

import org.springframework.boot.SpringApplication;
import org.sopt.korail.global.config.TimeConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        // JVM 기본 시간대를 지정
        TimeZone.setDefault(TimeZone.getTimeZone(TimeConfig.KST));
        SpringApplication.run(Main.class, args);
    }
}