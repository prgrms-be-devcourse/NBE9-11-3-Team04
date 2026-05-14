package com.back.devc.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * 프론트엔드(예: localhost:3000)에서 백엔드 API를 호출할 수 있도록 CORS 정책을 설정하는 클래스
 *
 * 현재 프로젝트는 프론트/백엔드가 서로 다른 Origin 에서 실행되기 때문에,
 * 브라우저가 차단하지 않도록 허용 Origin, 허용 메서드, 쿠키/인증 포함 여부 등을 명시적으로 설정
 */
@Configuration
public class WebCorsConfig {

    /**
     * application.yml 에서 CORS 허용 Origin 목록을 문자열로 받아옴
     *
     * 예)
     * custom.cors.allowed-origins=http://localhost:3000,http://127.0.0.1:3000
     *
     * 별도 설정이 없으면 기본값으로 http://localhost:3000 을 사용
     */
    @Value("${custom.cors.allowed-origins:http://localhost:3000}")
    private String allowedOriginsCsv;

    /**
     * 전역 CORS 설정 등록
     *
     * - 모든 경로(/**)에 대해 CORS 허용
     * - GET/POST/PATCH/DELETE/PUT/OPTIONS 허용
     * - 모든 헤더 허용
     * - 쿠키/인증 정보 포함 요청 허용
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // 브라우저에서 들어오는 교차 출처 요청에 대해 전역 정책을 적용한다.
                registry.addMapping("/**")
                        .allowedOrigins(parseOrigins())
                        .allowedMethods("GET", "POST", "PATCH", "DELETE", "PUT", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }

    /**
     * 쉼표(,)로 구분된 Origin 문자열을 실제 allowedOrigins 배열로 변환
     *
     * 공백은 제거하고, 비어 있는 값은 제외해서 Spring CORS 설정에 바로 넘길 수 있는 형태로 만듦
     */
    private String[] parseOrigins() {
        return Arrays.stream(allowedOriginsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toArray(String[]::new);
    }
}
