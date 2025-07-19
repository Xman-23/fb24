package com.example.demo.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
//JWT 유틸 클래스
public class JwtUtil {

	 // JWT 서명에 사용되는 비밀 키 (길이는 256비트 이상이어야 함)
	 private final String secret = "mySecretKeymySecretKeymySecretKeymySecretKey"; 
	
	 // 액세스 토큰 만료 시간: 3시간 (1000ms * 60s * 60m * 3h)
	 private final long accessExpirationMs = 1000 * 60 * 60 * 3;
	
	 // 리프레시 토큰 만료 시간: 7일
	 private final long refreshExpirationMs = 1000 * 60 * 60 * 24 * 7;
	
	 // 비밀 키를 이용해 HMAC SHA 알고리즘을 적용한 키 객체 생성
	 private final Key secretKey = Keys.hmacShaKeyFor(secret.getBytes());
	
	 // 로그 출력을 위한 로거 생성
	 private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
	
	 // ======= 액세스 토큰 생성 =======
	 // 이메일을 이용한 액세스 토큰 생성 (Subject: email)
	 public String generateToken(String email) {
	     Date now = new Date(); // 현재 시간
	     Date expiryDate = new Date(now.getTime() + accessExpirationMs); // 만료 시간 계산
	
	     // JWT 생성: 이메일을 subject로 하고, 발급 시간과 만료 시간을 설정
	     String token = Jwts.builder()
	             .setSubject(email) // token '주체'를 'email'로 설정
	             .setIssuedAt(now) //현재 날짜를 기준으로
	             .setExpiration(expiryDate) // 액세스 토큰 만료기간 설정
	             .signWith(secretKey, SignatureAlgorithm.HS256) // 서명 알고리즘 및 키 설정
	             .compact(); // 토큰 생성
	
	     return token;
	 }
	
	 // ======= 리프레시 토큰 생성 =======
	// 이메일을 이용한 리프레시 토큰 생성 (Subject: email)
	 public String generateRefreshToken(String email) {
	     Date now = new Date();
	     Date expiryDate = new Date(now.getTime() + refreshExpirationMs);
	
	     String token = Jwts.builder()
	             .setSubject(email) // token '주체'를 'email'로 설정
	             .setIssuedAt(now) // 현재 날짜를 기준으로
	             .setExpiration(expiryDate) // 리프레시 토큰 만료기간 설정
	             .signWith(secretKey, SignatureAlgorithm.HS256)
	             .compact();
	
	     return token;
	 }
	
	 // ======= 액세스 토큰 유효성 검증 =======
	 public boolean validateAccessToken(String token) {
	     return validateToken(token); // 내부 validateToken() 메서드 호출
	 }
	
	 // ======= 리프레시 토큰 유효성 검증 =======
	 public boolean validateRefreshToken(String token) {
	     return validateToken(token);
	 }
	
	 // ======= 토큰 유효성 검증 공통 메서드 =======
	 public boolean validateToken(String token) {

	     try {
	         Jwts.parserBuilder()
	             .setSigningKey(secretKey) // 서명 검증 키 설정
	             .setAllowedClockSkewSeconds(60) // 시계 오차 허용 (60초)
	             .build()
	             .parseClaimsJws(token); // 토큰 파싱 및 검증
	         return true;
	
	     } catch (ExpiredJwtException e) {
	         logger.warn("Token has expired: {}", e.getMessage());
	     } catch (UnsupportedJwtException e) {
	         logger.error("Unsupported JWT token: {}", e.getMessage());
	     } catch (MalformedJwtException e) {
	         logger.error("Malformed JWT token: {}", e.getMessage());
	     } catch (SignatureException e) {
	         logger.error("Invalid signature in JWT token: {}", e.getMessage());
	     } catch (IllegalArgumentException e) {
	         logger.error("Invalid JWT token: {}", e.getMessage());
	     } catch (JwtException e) {
	         logger.error("Unexpected JWT processing error: {}", e.getMessage());
	     }
	
	     return false; // 예외 발생 시 유효하지 않은 것으로 처리
	 }
	
	 // ======= 토큰에서 이메일(subject) 추출 =======
	 public String getEmailFromToken(String token) {
	     Claims claims = Jwts.parserBuilder()
	             .setSigningKey(secretKey)
	             .build()
	             .parseClaimsJws(token)
	             .getBody(); // 토큰 본문(claims) 추출
	
	     String subject = claims.getSubject(); // 'subject : email" 추출
	     return subject;
	 }
	
	 // ======= 토큰에서 모든 클레임 추출 =======
	 public Claims getClaimsFromToken(String token) {
	     Claims claims = Jwts.parserBuilder()
	             .setSigningKey(secretKey)
	             .build()
	             .parseClaimsJws(token)
	             .getBody();
	
	     return claims;
	 }
	
	 // ======= 임시 토큰 생성 (이메일 찾기용 - 30분) =======
	 public String createTempToken(String username, String residentNumber) {
	     Date now = new Date();
	     Date expiryDate = new Date(now.getTime() + 1000 * 60 * 30); // 30분 유효
	
	
	     // subject를 TEMP로 설정하고, username과 주민번호를 클레임으로 추가
	     String token = Jwts.builder()
	             .setSubject("TEMP")
	             .claim("username", username)
	             .claim("residentNumber", residentNumber)
	             .setIssuedAt(now)
	             .setExpiration(expiryDate)
	             .signWith(secretKey, SignatureAlgorithm.HS256)
	             .compact();
	
	     return token;
	 }
	
	 // ======= 임시 토큰 생성 (비밀번호 재설정용 - 30분) =======
	 public String createResetPasswordToken(String email, String username, String residentNumber) {
	     Date now = new Date();
	     Date expiryDate = new Date(now.getTime() + 1000 * 60 * 30); // 30분 유효
	
	
	     // subject를 RESET으로 설정하고, email, username, 주민번호를 클레임으로 추가
	     String token = Jwts.builder()
	             .setSubject("RESET")
	             .claim("email", email)
	             .claim("username", username)
	             .claim("residentNumber", residentNumber)
	             .setIssuedAt(now)
	             .setExpiration(expiryDate)
	             .signWith(secretKey, SignatureAlgorithm.HS256)
	             .compact();
	
	     return token;
	 }
}