package com.example.demo.jwt;

import com.example.demo.domain.Member;
import com.example.demo.repository.MemberRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    public JwtAuthenticationFilter(JwtUtil jwtUtil, MemberRepository memberRepository) {
        this.jwtUtil = jwtUtil;
        this.memberRepository = memberRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            // 1. JWT 토큰 추출
            String token = parseJwt(request);
            logger.info("JwtAuthenticationFilter doFilterInternal token  :  " + token);
            // =======================
            // 임시 토큰(TEMP : 이메일, RESET : 비밀번호) 검사 시작
            if (token != null) {
                Claims claims;

                try {
                    // 2. 토큰에서 Claims 추출
                    claims = jwtUtil.getClaimsFromToken(token);
                    logger.info("JwtAuthenticationFilter doFilterInternal claims  :  " + claims);

                } catch (ExpiredJwtException e) {
                    // 3. 만료된 토큰에서 subject 추출
                    Claims expiredClaims = e.getClaims();
                    String subject = expiredClaims != null ? expiredClaims.getSubject() : null;

                    // 3-1. TEMP, RESET 토큰은 만료되었어도 인증 없이 통과시킴
                    if ("TEMP".equals(subject) || "RESET".equals(subject)) {
                        filterChain.doFilter(request, response); // 인증 없이 통과
                        return;
                    }

                    // 3-2. 일반 토큰이 만료된 경우는 인증 실패
                    throw e; // 이후 catch 블록에서 처리됨
                }

                String subject = claims.getSubject(); // subject는 email 또는 TEMP 등

                // 5. TEMP || RESET 토큰은 인증 생략하고 통과시킴
                if ("TEMP".equals(subject) || "RESET".equals(subject)) {
                    logger.info("TEMP token validated successfully. No authentication required.");

                    if (jwtUtil.validateToken(token)) {
                        logger.debug("TEMP || RESET token is still valid."); // TEMP 토큰이 아직 유효한 경우 로그만 남김
                    }

                    filterChain.doFilter(request, response); // 인증 없이 통과
                    return;
                }
                // 임시 토큰 검사 끝
                // =======================

                // 정식 로그인 토큰 (3시간 짜리 추후 -> 30분으로 변경)
                // 6. 일반 토큰 유효성 검사
                if (jwtUtil.validateToken(token)) {
                    String email = jwtUtil.getEmailFromToken(token).trim(); // 이메일 추출

                    // 7. 이메일로 사용자 조회
                    Member member = memberRepository.findByEmail(email)
                            .orElseThrow(() -> {
                                return new IllegalArgumentException("User not found.");
                            });

                    // 8. UserDetails(사용자정보) 객체 생성
                    CustomUserDetails userDetails = new CustomUserDetails(member);

                    // 9. 인증(인증 성공) 객체 생성
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    																							userDetails,
                    																							null,
                    																							userDetails.getAuthorities()
                    																						    );

                    // 10. 요청 정보와 함께 인증 객체 설정
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 11. SecurityContext에 인증 등록
                    // @AuthenticationPrincipal 등으로 인증된 사용자 정보를 참조 가능
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }

            }
        } catch (IllegalArgumentException e) {
            // 사용자를 찾을 수 없는 경우
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed: user not found.");
            return;

        } catch (ExpiredJwtException e) {
            // 일반 토큰이 만료된 경우
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token has expired.");
            return;

        } catch (Exception e) {
            // 기타 예외
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed.");
            return;
        }

        // 다음 필터로 넘김
        filterChain.doFilter(request, response);
    }

    // =======================
    // Authorization 헤더에서 Bearer 토큰 추출
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            String token = headerAuth.substring(7); // "Bearer " 다음부터 잘라냄
            return token;
        }

        return null;
    }
}