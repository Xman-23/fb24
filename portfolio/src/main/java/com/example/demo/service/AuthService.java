package com.example.demo.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.authdto.AuthTokenResponseDTO;
import com.example.demo.domain.Member;
import com.example.demo.domain.RefreshToken;
import com.example.demo.jwt.JwtUtil;
import com.example.demo.repository.MemberRepository;
import com.example.demo.repository.RefreshTokenRepository;

import jakarta.transaction.Transactional;

@Service
public class AuthService {

	private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    // 생성자 주입
    //'@Autpwired'안에 'Bean'이 포함되어있어 객체 생명주기 관리 
    @Autowired
    public AuthService(MemberRepository memberRepository, 
    					 BCryptPasswordEncoder passwordEncoder, 
    					 JwtUtil jwtUtil,
    					 RefreshTokenRepository refreshTokenRepository) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    // Request 에서 'null'을 가지고 있을 경우 안전하게 trim 처리하는 메서드
    private String safeTrim(String s) {
    	return (s == null) ? "" : s.trim();
    }

    // 로그인 Service
    @Transactional
    public AuthTokenResponseDTO login(String trimEmail, String trimPassword) {

    	// DTO
        String dtoEmail = trimEmail;
        String dtoPassword = trimPassword;

        logger.info("Attempting login for email: {}", dtoEmail);

        Member member = memberRepository.findByEmail(dtoEmail)
            .orElseThrow(() -> {
                logger.warn("Login failed - email not found: {}", dtoEmail);
                return new IllegalArgumentException("Email not registered.");
            });

        // DB
        String dbTrimPassword = safeTrim(member.getPassword());

        // 첫번쨰 파라미터 암호화 되지 않은 DTO Password, 두번째 파라미터 암호화된 DB Password
        if (!passwordEncoder.matches(dtoPassword, dbTrimPassword)) {
            logger.warn("Login failed - incorrect password for email: {}", trimEmail);
            throw new IllegalArgumentException("Incorrect password.");
        }

        String accessToken = jwtUtil.generateToken(dtoEmail); // 'Subject'가 'email'인 '액세스 토큰' 생성 
        String refreshToken = jwtUtil.generateRefreshToken(dtoEmail); // 'Subject'가 'email'인 '리프레시 토큰' 생성

        // DB에 저장되어있는 이메일 삭제
        refreshTokenRepository.deleteByEmail(dtoEmail);

        // RefreshToken Entity
        RefreshToken refreshTokenEntity = new RefreshToken();

        //===================================================
        // RefreshToken Entity Setting
        // 리프레시토큰 Setting
        refreshTokenEntity.setToken(safeTrim(refreshToken));
        // 이메일 Setting
        refreshTokenEntity.setEmail(safeTrim(trimEmail));
        // 현재 시간을 기준으로 '+7일' 짜리 리프레시 토큰 Setting
        refreshTokenEntity.setExpiryDate(LocalDateTime.now().plusDays(7));
        // RefreshToken Entity End
        //===================================================

        // DB Insert
        refreshTokenRepository.save(refreshTokenEntity);

        logger.info("Login successful - tokens issued for email: {}", trimEmail);
        return new AuthTokenResponseDTO(accessToken, refreshToken);
    }

    // 액세스 토큰 재발급 Service
    public AuthTokenResponseDTO refreshAccessToken(String trimRefreshToken) {
        logger.info("Refreshing access token...");

        RefreshToken tokenEntity = refreshTokenRepository.findByToken(trimRefreshToken)
            .orElseThrow(() -> {
                logger.warn("Refresh failed - token not found: {}", trimRefreshToken);
                return new IllegalArgumentException("Invalid refresh token.");
            });

        // expirationTime이 지금보다 이전이면 true → 만료된 상태
        if (tokenEntity.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(tokenEntity);
            logger.warn("Refresh token expired: {}", trimRefreshToken);
            throw new IllegalArgumentException("Refresh token has expired.");
        }
        // '7일'리프레시 토큰을 이용하여, 'subject : email' 추출
        String email = jwtUtil.getEmailFromToken(trimRefreshToken);
        // 'email'을 활용하여 다시 '액세서 토큰' 생성
        String newAccessToken = jwtUtil.generateToken(email);

        logger.info("Access token successfully reissued for email: {}", email);
        return new AuthTokenResponseDTO(newAccessToken, trimRefreshToken);
    }

    // 로그아웃 Service
    @Transactional 
    public void logout(String trimEmail) {
    	refreshTokenRepository.deleteByEmail(trimEmail);
    }
}
