package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.demo.jwt.CustomUserDetails;
import com.example.demo.jwt.JwtAuthenticationFilter;
import com.example.demo.jwt.JwtUtil;
import com.example.demo.repository.member.MemberRepository;
import com.example.demo.security.CustomAuthenticationEntryPoint;

@Configuration
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
            // SecurityConfig에서 권한 설정 부분
            // .hasRole("ADMIN") 은 내부적으로 "ROLE_ADMIN" 권한을 체크하기 때문에 getAuthorities()가 "ROLE_ADMIN"을 반환해야 함
            // 따라서 관리자 전용 URL에 대해 ADMIN 권한만 접근 가능하도록 설정
            .requestMatchers("/boards/admin/create-board",
            				 "/boards/admin/create-board",
                             "/boards/admin/*",
                             "/boards/admin/**").hasRole("ADMIN")

            // * 비로그인 사용자도 접근 가능 *
            .requestMatchers("/members/signup",
                             "/members/check-email",
                             "/members/check-nickname",
                             "/members/reset-password-token",
                             "/members/reset-password",
                             "/members/find-email",
                             "/members/show-email",
                             "/auth/refresh",
                             "/auth/login",
                             "/boards/*",
                             "/boards/**").permitAll()

            // * 로그인 사용자만 접근 가능  (접근시 'Header'에 액세스 토큰 꼭 보내야함)* 
            .requestMatchers("/members/me",
                    		 "/auth/refresh",
                             "/auth/logout").authenticated()
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
