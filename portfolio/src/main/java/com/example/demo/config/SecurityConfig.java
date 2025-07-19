package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.demo.jwt.JwtAuthenticationFilter;
import com.example.demo.jwt.JwtUtil;
import com.example.demo.repository.MemberRepository;
import com.example.demo.security.CustomAuthenticationEntryPoint;

@Configuration
public class SecurityConfig {

	private final JwtUtil jwtUtil;
	private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
	private final MemberRepository memberRepository;

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
        http
            .csrf().disable() // REST API는 보통 CSRF 비활성화
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS) // JWT는 세션 사용 안함
            .and()
            .authorizeHttpRequests()
            	//403 -> 로그인 인증은 성공했지만, 해당 페이지에 접근권한이 없을경우에는 403에러 발생.
            	.requestMatchers("/members/signup",
            					 "/members/check-nickname",
            					 "/members/reset-password-token",
            					 "/members/reset-password",
            					 "/members/me",
            			         "/members/find-email",
            			         "/members/show-email",
            			         "/auth/login",
            			         "/auth/refresh",
            			         "/auth/logout").permitAll() // 회원가입, 로그인 누구나 접근가능
                .anyRequest().authenticated() // 나머지는 인증 필요
            .and()
            //예외 처리 핸들링
            .exceptionHandling()
            					.authenticationEntryPoint(customAuthenticationEntryPoint) //401 에러 등록
            .and()
            // 모든 요청은 JwtAuthenticationFilter를 먼저 거쳐서 JWT 인증 여부를 검사한 후,
            // 필요 시 UsernamePasswordAuthenticationFilter(로그인 필터)로 넘어간다
            .addFilterBefore(new JwtAuthenticationFilter(jwtUtil, memberRepository), UsernamePasswordAuthenticationFilter.class);// JWT 인증 필터를 Spring Security 필터 앞에 등록
        return http.build();
    }
    // AuthenticationManager Bean 등록
    //인증 관련 처리를 위해 등록 (현재는 직접 사용 X)
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
