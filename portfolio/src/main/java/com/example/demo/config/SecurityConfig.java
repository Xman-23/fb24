package com.example.demo.config;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.demo.jwt.JwtAuthenticationFilter;
import com.example.demo.jwt.JwtUtil;
import com.example.demo.repository.member.MemberRepository;
import com.example.demo.security.CustomAuthenticationEntryPoint;

@Configuration
// '@PreAuthorize("hasRole('ADMIN')")' 활성화 시키기 위한,
// '@EnableGlobalMethodSecurity' 어노테이션
@EnableGlobalMethodSecurity(prePostEnabled =  true)
public class SecurityConfig {

	private final JwtUtil jwtUtil;
	private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
	private final MemberRepository memberRepository;

	private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

	public SecurityConfig(JwtUtil jwtUtil, 
			              CustomAuthenticationEntryPoint customAuthenticationEntryPoint,
			              MemberRepository memberRepository) {
		this.jwtUtil = jwtUtil;
		this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;
		this.memberRepository = memberRepository;
	}

	//비밀번호 암호화
	//직접 자바 설정 파일(@Configuration)에서 메서드로 객체를 만들어 빈으로 등록하고 싶을 때
	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		//반환하는 객체를 스프링 컨테이너에 직접 등록한다
		//직접 등록 한다는 반환(return)하는 객체(new Class)를 빈으로 등록하는 것
		return new BCryptPasswordEncoder();
	}

	//토큰 권한 설정
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    	logger.info("SecurityFilterChain filterChain Start");
        http
            .csrf().disable() // REST API는 보통 CSRF 비활성화
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS) // JWT는 세션 사용 안함
            .and()
            .authorizeHttpRequests()

            //403 -> 로그인 인증은 성공했지만, 해당 페이지에 접근권한이 없을경우에는 403에러 발생.

            // * 관리자 전용 * (접근시 'Header'에 액세스 토큰 꼭 보내야함)
            // SecurityConfig에서 권한 설정 부분 ('접두어 ROLE_'을 제외한 "ADMIN", "USER"만 명시)
            // .hasRole("ADMIN") 은 내부적으로 "ROLE_ADMIN" 권한을 체크하기 때문에 getAuthorities()가 "ROLE_ADMIN"을 반환해야 함
            // 따라서 관리자 전용 URL에 대해 ADMIN 권한만 접근 가능하도록 설정
            // 관리자 전용 ([\\d]+ : 숫자 하나 이상(즉, 문자열X))
            .requestMatchers("/boards/admin/**").hasRole("ADMIN")
            .requestMatchers("/posts/{postId:[\\d]+}/pin").hasRole("ADMIN")

            // 비로그인도 접근 가능
            .requestMatchers(
                "/auth/login",
                "/members/signup",
                "/members/check-email",
                "/members/check-nickname",
                "/members/reset-password-token",
                "/members/reset-password",
                "/members/find-email",
                "/members/show-email",

                "/boards/**",

                // 게시글 관련 - 비로그인 허용 목록 ([\\d]+ : 숫자 하나 이상(즉, 문자열X))
                "/posts/{postId:[\\d]+}",                 // 게시글 단건 조회, 댓글 트리조회
                "/posts/board/**",                        // 게시판별 목록 조회, 자식 게시판 정렬 조회
                "/posts/boards/**",						  // 자식게시판 정렬, 부모게시판 보기
                "/posts/search",                          // 키워드 검색
                "/posts/notices",                         // 전체 공지 조회
                "/posts/notices/pinned",                  // 상단 고정 공지
                "/posts/{postId:[\\d]+}/images",          // 게시글 이미지 조회
                "/posts/parent-boards/**",			      // 부모 게시판 정렬
                "/posts/author/**",
 
                // 댓글
                "/comments/post/{postId:[\\d]+}"		 // 댓글 트리구조(정렬) 조회

            ).permitAll()

            // 로그인 사용자만 접근 가능 ([\\d]+ : 숫자 하나 이상(즉, 문자열X))
            .requestMatchers(
            	// 게시글 
                "/posts",                                // 게시글 생성 (POST)
                "/posts/{postId:[\\d]+}",                // 게시글 수정 (PATCH), 삭제 (DELETE)
                "/posts/{postId:[\\d]+}/report",         // 댓글 신고
                "/posts/{postId:[\\d]+}/view",           // 조회수 증가
                "/posts/{postId:[\\d]+}/images/order",   // 이미지 정렬
                "/posts/{postId:[\\d]+}/images/{imageId:[\\d]+}",         // 이미지 단건 삭제
                "/posts/{postId:[\\d]+}/images",		 // 이미지 모두 삭제

                // 회원
                "/members/me",
                "/auth/refresh",
                "/auth/logout",

                // 댓글
                "/comments",							// 댓글 생성
                "/comments/{commentId:[\\d]+}",			// 댓글 수정, 삭제
                "/comments/{commentId:[\\d]+}/report",	// 댓글 신고

                // 알림
                "/notifications/**",					// 알림

                // 게시글 리액션(좋아요, 싫어요)
                "/postreactions/**",					// 게시글 리액션

                // 댓글 리액션(좋아요, 싫어요)
                "/commentreactions/**"					// 댓글 리액션

            ).authenticated()
            // 나머지는 인증 필요
            .anyRequest().authenticated() 
            .and()
            //예외 처리 핸들링
            .exceptionHandling()
            					.authenticationEntryPoint(customAuthenticationEntryPoint) //401 에러 등록
            .and()
            // 모든 요청은 JwtAuthenticationFilter를 먼저 거쳐서 JWT 인증 여부를 검사한 후,
            // 필요 시 UsernamePasswordAuthenticationFilter(로그인 필터)로 넘어간다
            .addFilterBefore(new JwtAuthenticationFilter(jwtUtil, memberRepository), UsernamePasswordAuthenticationFilter.class);// JWT 인증 필터를 Spring Security 필터 앞에 등록
        logger.info("SecurityFilterChain filterChain End");
        return http.build();
    }

    // AuthenticationManager Bean 등록
    //인증 관련 처리를 위해 등록 (현재는 직접 사용 X)
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
