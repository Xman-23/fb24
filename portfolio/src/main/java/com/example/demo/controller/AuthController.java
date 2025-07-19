package com.example.demo.controller;

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

import com.example.demo.authdto.AuthLoginDto;
import com.example.demo.authdto.AuthRefreshTokenRequestDTO;
import com.example.demo.authdto.AuthTokenResponseDTO;
import com.example.demo.jwt.CustomUserDetails;
import com.example.demo.service.AuthService;
import com.example.demo.service.MemberService;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private static final Logger logger = LoggerFactory.getLogger(MemberController.class);

    @Autowired
    public AuthController(AuthService authService) {
    	this.authService = authService;
    }

    // Request 에서 'null'을 가지고 있을 경우 안전하게 trim 처리하는 메서드
    private String safeTrim(String s) {
    	return (s == null) ? "" : s.trim();
    }

    // 로그인 API엔드포인트
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid AuthLoginDto authLoginDto, BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
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

        return ResponseEntity.ok(tokens);

    }

    // 액세스 토큰 재발급 API엔드포인트
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody @Valid AuthRefreshTokenRequestDTO authRefreshTokenRequestDTO,
                                          BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            logger.warn("Refresh token failed - missing refresh token.");
            return ResponseEntity.badRequest().body("Refresh token is required.");
        }

        String trimRefreshToken = safeTrim(authRefreshTokenRequestDTO.getRefreshToken());

        try {
            AuthTokenResponseDTO tokens = authService.refreshAccessToken(trimRefreshToken);
            logger.info("Access token reissued successfully.");
            return ResponseEntity.ok(tokens);
        } catch (IllegalArgumentException e) {
            logger.warn("Refresh token invalid: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Refresh token error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server error.");
        }
    }

    // 로그아웃 API엔드포인트
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal CustomUserDetails customUserDetails) {

    	String trimEmail = safeTrim(customUserDetails.getEmail());
    	authService.logout(trimEmail);

    	return ResponseEntity.ok("로그아웃 되었습니다.");
    }
}
