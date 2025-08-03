package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
// '뷰'에서 '서버'의 이미지 접근을 위한 클래스
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload.base-path}")
    private String basePath;

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {

		String resourceLocation = "file:///" + basePath + "/";

		registry.addResourceHandler("/images/**") // 요청 경로
		        .addResourceLocations(resourceLocation); // 셀제 로컬 경로
		// URL 요청: /images/abc.jpg → 실제 로컬 경로: C:/upload/image/abc.jpg
	}

}
