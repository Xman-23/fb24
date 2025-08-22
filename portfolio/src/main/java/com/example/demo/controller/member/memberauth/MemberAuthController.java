package com.example.demo.controller.member.memberauth;

import org.slf4j.Logger;



import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.member.memberauth.AuthLoginDTO;
import com.example.demo.dto.member.memberauth.AuthTokenResponseDTO;
import com.example.demo.jwt.CustomUserDetails;
import com.example.demo.service.member.memberauth.MemberAuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class MemberAuthController {

    private final MemberAuthService authService;

    private static final Logger logger = LoggerFactory.getLogger(MemberAuthController.class);

    // Request 에서 'null'을 가지고 있을 경우 안전하게 trim 처리하는 메서드
    private String safeTrim(String s) {
    	return (s == null) ? "" : s.trim();
    }

    //*************************************************** API START ***************************************************//

    // 로그인 API엔드포인트
    @PostMapping("/login")
    public ResponseEntity<?> login(HttpServletResponse response,
    		                       @RequestBody @Valid AuthLoginDTO authLoginDto, 
    		                       BindingResult bindingResult) {
    	
    	logger.info("AuthController login() Start");
        if (bindingResult.hasErrors()) {
        	logger.error("AuthController login() Error : 'AuthLoginDTO'가 유효하지 않습니다.");
            return ResponseEntity.badRequest().body("로그인 입력값이 유효하지 않습니다.");
        }

        // DTO Trim
        String trimEmail = safeTrim(authLoginDto.getEmail()); //Email(ID)
        String trimPassword = safeTrim(authLoginDto.getPassword()); //Password

        AuthTokenResponseDTO tokens = null;

        try {
            tokens = authService.login(trimEmail, trimPassword);
            logger.info("AuthController Login successful - email: {}", trimEmail);
        } catch (IllegalArgumentException e) {
            logger.warn("AuthController Login failed - reason: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 정보가 유효하지 않습니다.");
        }
        // HTTP Only Cookie로 리프레시 토큰 전달
        ResponseCookie cookie = ResponseCookie.from("refreshToken", tokens.getRefreshToken())
                                              .httpOnly(true)
                                              .secure(true)
                                              .path("/")
                                              .maxAge(7 * 24 * 60 * 60)
                                              .sameSite("Strict")
                                              .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
 
        logger.info("AuthController login() Success End");
        return ResponseEntity.ok(tokens);

    }

    // 액세스 토큰 재발급 API엔드포인트
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization")String refreshToken) {

    	logger.info("AuthController login() Start");

    	if(refreshToken == null) {
        	logger.error("MemberAuthController refreshToken refreshToken : null ");
        	return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("null");
    	}

        if (refreshToken != null) {
        	if(!refreshToken.startsWith("Bearer ")) {
            	logger.error("MemberAuthController refreshToken refreshToken : 유효하지 않은 토큰 형식입니다. ");
            	return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 토큰 형식입니다.");
        	}
        }

        String trimSubStringRefreshToken = safeTrim(refreshToken.substring(7));
        AuthTokenResponseDTO tokens = null;

        try {
            tokens = authService.refreshAccessToken(trimSubStringRefreshToken);
            logger.info("Access token reissued successfully.");
        } catch (IllegalArgumentException e) {
            logger.warn("Refresh token invalid: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }

        logger.info("AuthController login() Success End");
        return ResponseEntity.ok(tokens);
    }

    // 로그아웃 API엔드포인트
    // 로그아웃은 리프레시 토큰을 DB에서 지우는 작업이라 상태 변화 생김
    // 조회(GET) 아님 → POST로 보내야 함
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response,
    		                        @AuthenticationPrincipal CustomUserDetails customUserDetails) {

    	logger.info("AuthController logout() Start");

        // 1. HTTP Only 쿠키 삭제 (maxAge=0)
        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                                                    .httpOnly(true)
                                                    .secure(true)
                                                    .path("/")
                                                    .maxAge(0)       // 만료
                                                    .sameSite("Strict")
                                                    .build();

        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
 
    	logger.info("AuthController logout() Success End");
    	return ResponseEntity.ok("로그아웃 되었습니다.");
    }

    //*************************************************** API END ***************************************************//
}
