package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
// '뷰'에서 '서버'의 이미지 접근을 위한 클래스
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload.base-path}")
    private String basePath;

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {

		// file:// 프로토콜 /C:/upload/images/ 로컬경로
		String resourceLocation = "file:///" + basePath + "/";

		registry.addResourceHandler("/images/**") // 요청 경로
		        .addResourceLocations(resourceLocation); // 셀제 로컬 경로
		// URL 요청: /images/abc.jpg → 실제 로컬 경로: C:/upload/image/abc.jpg
	}

	@Override
	public void addViewControllers(ViewControllerRegistry viewControllerRegistry) {
		
		//********************************************************Main Start*****************************************************
		// "/(루트)"접속시 index.html로 Forward
		viewControllerRegistry.addViewController("/").setViewName("forward:/main.html");

		viewControllerRegistry.addViewController("/main/post/{postId:\\d+}").setViewName("forward:/main_post_update.html");
		

		// 통합검색(쿼리 사용)
		viewControllerRegistry.addViewController("/search").setViewName("forward:/total/total_search.html");
		//********************************************************Main End*****************************************************

		//********************************************************Member Start*****************************************************

		// "signin"접속시 member_signin.html로 Forward
		viewControllerRegistry.addViewController("/signin").setViewName("forward:/member/member_signin.html");

		// "signup_consent"접속시 member_signup_consent.html로 Forward
		viewControllerRegistry.addViewController("/signup_consent").setViewName("forward:/member/member_signup_consent.html");

		// "signup"접속시 member_signup.html로 Forward
		viewControllerRegistry.addViewController("/signup").setViewName("forward:/member/member_signup.html");

		// "find_id"접속시  member_find_id_one.html로 Forward
		viewControllerRegistry.addViewController("/find_id_one").setViewName("forward:/member/member_find_id_one.html");

		// "find_find_id_two"접속시  member_find_id_two.html로 Forward
		viewControllerRegistry.addViewController("/find_id_two").setViewName("forward:/member/member_find_id_two.html");
	
		// "reset_password_one"접속시  member_reset_password_one.html로 Forward
		viewControllerRegistry.addViewController("/reset_password_one").setViewName("forward:/member/member_reset_password_one.html");

		// "reset_password_two"접속시 member_reset_password_two.html로 Forward
		viewControllerRegistry.addViewController("/reset_password_two").setViewName("forward:/member/member_reset_password_two.html");

		// "member_me"접속시 member_me.html로 Forward
		viewControllerRegistry.addViewController("/member_me").setViewName("forward:/member/member_me.html");

		// "member_me_update'접속시 member_me_update.html로 Forward
		viewControllerRegistry.addViewController("/member_me_update").setViewName("forward:/member/member_me_update.html");

		//********************************************************Member End*****************************************************

		//********************************************************Board Start*****************************************************

		// 공지게시판
		viewControllerRegistry.addViewController("/board_notice/**").setViewName("forward:/board/board_notice/board_notice.html");
		
		// 부모게시판
		viewControllerRegistry.addViewController("/board_popular/**").setViewName("forward:/board/board_parent/board_parent.html");

		// 자식게시판 
		viewControllerRegistry.addViewController("/board_normal/**").setViewName("forward:/board/board_child/board_child.html");

		// 부모게시판 생성
		viewControllerRegistry.addViewController("/board_parent_create").setViewName("forward:/board/board_parent/board_parent_create.html");

		// 부모게시판 수정
		viewControllerRegistry.addViewController("/board_parent_update/**").setViewName("forward:/board/board_parent/board_parent_update.html");

		// 자식게시판 생성
		viewControllerRegistry.addViewController("/board_child_create/**").setViewName("forward:/board/board_child/board_child_create.html");

		// 자식게시판 수정
		viewControllerRegistry.addViewController("/board_child_update/**").setViewName("forward:/board/board_child/board_child_update.html");

		//********************************************************Board End*****************************************************

		//********************************************************Post Start*****************************************************
		viewControllerRegistry.addViewController("/main/{postId:\\d+}").setViewName("forward:/main_post.html");

		viewControllerRegistry.addViewController("/board/{boardId:\\d+}/popular/{postId:\\d+}").setViewName("forward:/post/post_parent/post_parent.html");
		viewControllerRegistry.addViewController("/board/{boardId:\\d+}/popular/post").setViewName("forward:/post/post_parent/post_parent_create.html");
		viewControllerRegistry.addViewController("/board/{boardId:\\d+}/popular/post/{postId:\\d+}").setViewName("forward:/post/post_parent/post_parent_update.html");

		viewControllerRegistry.addViewController("/board/{boardId:\\d+}/normal/{postId:\\d+}").setViewName("forward:/post/post_child/post_child.html");
		viewControllerRegistry.addViewController("/board/{boardId:\\d+}/normal/popular/{postId:\\d+}").setViewName("forward:/post/post_child/post_child_popular.html");
		viewControllerRegistry.addViewController("/board/{boardId:\\d+}/normal/post").setViewName("forward:/post/post_child/post_child_create.html");
		viewControllerRegistry.addViewController("/board/{boardId:\\d+}/normal/post/{postId:\\d+}").setViewName("forward:/post/post_child/post_child_update.html");

		viewControllerRegistry.addViewController("/board/{boardId:\\d+}/notice/{postId:\\d+}").setViewName("forward:/post/post_notice/post_notice.html");
		viewControllerRegistry.addViewController("/{boardId:\\d+}/notice/post").setViewName("forward:/post/post_notice/post_notice_create.html");
		viewControllerRegistry.addViewController("/{boardId:\\d+}/notice/post/{postId:\\d+}").setViewName("forward:/post/post_notice/post_notice_update.html");
		//********************************************************Post End*****************************************************

		//********************************************************Notification Start*****************************************************
		viewControllerRegistry.addViewController("/notification").setViewName("forward:/notification/notification.html");
		//********************************************************Notification End*****************************************************
		
	}
}
