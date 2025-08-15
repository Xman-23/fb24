package com.example.demo.controller.member.memberauth;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.member.memberauth.AuthLoginDTO;
import com.example.demo.dto.member.memberauth.AuthRefreshTokenRequestDTO;
import com.example.demo.dto.member.memberauth.AuthTokenResponseDTO;
import com.example.demo.jwt.CustomUserDetails;
import com.example.demo.service.member.memberauth.MemberAuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class MemberAuthController {

    private final MemberAuthService authService;
    private static final Logger logger = LoggerFactory.getLogger(MemberAuthController.class);

    @Autowired
    public MemberAuthController(MemberAuthService authService) {
    	this.authService = authService;
    }

    // Request 에서 'null'을 가지고 있을 경우 안전하게 trim 처리하는 메서드
    private String safeTrim(String s) {
    	return (s == null) ? "" : s.trim();
    }

    //*************************************************** API START ***************************************************//

    // 로그인 API엔드포인트
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid AuthLoginDTO authLoginDto, BindingResult bindingResult) {
    	
    	logger.info("AuthController login() Start");
        if (bindingResult.hasErrors()) {
        	logger.error("AuthController login() Error : 'AuthLoginDTO'가 유효하지 않습니다.");
            return ResponseEntity.badRequest().body("Invalid input.");
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
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password.");
        } catch (Exception e) {
            logger.error("AuthController Login error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server error.");
        }
        logger.info("AuthController login() Success End");
        return ResponseEntity.ok(tokens);

    }

    // 액세스 토큰 재발급 API엔드포인트
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody @Valid AuthRefreshTokenRequestDTO authRefreshTokenRequestDTO,
                                          BindingResult bindingResult) {

    	logger.info("AuthController login() Start");

        if (bindingResult.hasErrors()) {
            logger.error("Refresh token failed - missing refresh token.");
            return ResponseEntity.badRequest().body("Refresh token is required.");
        }

        logger.info("AuthController  refreshToken() 실패 - missing refresh token.");
        String trimRefreshToken = safeTrim(authRefreshTokenRequestDTO.getRefreshToken());

        AuthTokenResponseDTO tokens = null;

        try {
            tokens = authService.refreshAccessToken(trimRefreshToken);
            logger.info("Access token reissued successfully.");
        } catch (IllegalArgumentException e) {
            logger.warn("Refresh token invalid: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Refresh token error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server error.");
        }

        logger.info("AuthController login() Success End");
        return ResponseEntity.ok(tokens);
    }

    // 로그아웃 API엔드포인트
    // 로그아웃은 리프레시 토큰을 DB에서 지우는 작업이라 상태 변화 생김
    // 조회(GET) 아님 → POST로 보내야 함
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal CustomUserDetails customUserDetails) {

    	logger.info("AuthController logout() Start");

    	String trimEmail = safeTrim(customUserDetails.getEmail());
    	authService.logout(trimEmail);
 
    	logger.info("AuthController logout() Success End");
    	return ResponseEntity.ok("로그아웃 되었습니다.");
    }

    //*************************************************** API END ***************************************************//
}
