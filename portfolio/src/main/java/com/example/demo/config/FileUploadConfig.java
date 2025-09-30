package com.example.demo.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

@Configuration
public class FileUploadConfig {

    // 파일 크기, 전체 요청 크기, 임시 임계치 설정
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.ofMegabytes(10));       // 파일 1개 최대 10MB
        factory.setMaxRequestSize(DataSize.ofMegabytes(50));    // 전체 요청 최대 50MB
        factory.setFileSizeThreshold(DataSize.ofMegabytes(2));  // 임시 저장 임계치
        return factory.createMultipartConfig();
    }

    // Tomcat multipart 파일 개수 제한 설정
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> factory.addContextCustomizers(context -> {
            // multipart 파일 최대 개수 20개로 설정 (필요시 조정 가능)
            context.getServletContext().setAttribute(
                "org.apache.tomcat.util.http.fileupload.FileUploadBase.fileCountMax", 20L
            );
        });
    }
}