package com.back.devc.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 업로드된 파일에 접근할 수 있도록 정적 리소스 경로를 연결하는 설정
 *
 * 현재 댓글 첨부파일은 서버의 uploads 디렉토리에 저장되므로,
 * 브라우저에서 /uploads/** 경로로 접근했을 때 실제 파일이 열릴 수 있도록 ResourceHandler 를 등록
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * /uploads/** 요청을 실제 로컬 uploads 폴더와 매핑
     *
     * 예)
     * - DB 에 저장된 fileUrl: /uploads/comments/xxx.jpg
     * - 브라우저 요청 경로: http://localhost:8080/uploads/comments/xxx.jpg
     * - 실제 서버 파일 위치: 프로젝트루트/uploads/comments/xxx.jpg
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 실행 중인 프로젝트 루트 기준으로 uploads 폴더 경로를 잡는다.
        Path uploadPath = Paths.get(System.getProperty("user.dir"), "uploads");
        // Spring ResourceHandler 에 넘길 수 있도록 file URI 형태로 변환한다.
        String uploadDir = uploadPath.toUri().toString();

        // /uploads/** 로 들어온 요청을 실제 uploads 폴더에서 찾도록 연결한다.
        // 이 설정이 있어야 댓글 첨부파일 URL을 프론트에서 그대로 열 수 있다.
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadDir);
    }
}