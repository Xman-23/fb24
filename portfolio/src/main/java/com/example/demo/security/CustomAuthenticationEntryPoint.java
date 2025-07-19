package com.example.demo.security;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

//403에러를 -> 401 Unauthorized 응답을 직접 보내는 클래스
//403 : 권한 ,인증 에러/ 401 : 인증 에러
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

	@Override
	public void commence(HttpServletRequest request, 
						 HttpServletResponse response,
						 AuthenticationException authException) throws IOException, ServletException {
		//401 상태코드와 메시지 설정
		response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "인증이 필요합니다" );
		/*'Json' 형태로 바디 응답 받기
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType("application/json;charset=UTF-8");
		response.getWriter().write("{\"error\" : \"인증이 필요합니다\"}");
		*/
	}
}
