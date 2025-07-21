package com.example.demo.jwt;

import com.example.demo.domain.member.Member;
import com.example.demo.repository.member.MemberRepository;

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

    	logger.info("JwtAuthenticationFilter doFilterInternal Start");
        try {
            // 1. JWT 토큰 추출
            String token = parseJwt(request);
            logger.info("JwtAuthenticationFilter doFilterInternal token  :" + token);
            // ================================================================================================================
            // 임시 토큰(TEMP : 이메일, RESET : 비밀번호) 검사 시작
            logger.info("JwtAuthenticationFilter doFilterInternal 임시토큰 검사 시작 'IF문' Start");
            if (token != null) {
                Claims claims = null;
                logger.info("JwtAuthenticationFilter doFilterInternal 'IF' Pass" );
                try {
                    // 2. 토큰에서 Claims 추출
                	logger.info("JwtAuthenticationFilter doFilterInternal try Start");
                    claims = jwtUtil.getClaimsFromToken(token);
                    logger.info("JwtAuthenticationFilter doFilterInternal try claims  :" + claims);
                    logger.info("JwtAuthenticationFilter doFilterInternal try문 End");
                } catch (ExpiredJwtException e) {
                	logger.info("JwtAuthenticationFilter doFilterInternal ExpiredJwtException catch Start");
                    // 3. 만료된 토큰에서 subject 추출
                    Claims expiredClaims = e.getClaims();
                    logger.info("JwtAuthenticationFilter doFilterInternal expiredClaims  :" + expiredClaims);
                    String subject = expiredClaims != null ? expiredClaims.getSubject() : null;
                    logger.info("JwtAuthenticationFilter doFilterInternal subject  :" + subject);
                    // 3-1. TEMP, RESET 토큰은 만료되었어도 인증 없이 통과시킴
                    if ("TEMP".equals(subject) || "RESET".equals(subject)) {
                    	logger.info("JwtAuthenticationFilter doFilterInternal ExpiredJwtException catch 'IF' Start" + subject);
                        filterChain.doFilter(request, response); // 인증 없이 통과
                        logger.info("JwtAuthenticationFilter doFilterInternal ExpiredJwtException catch 'IF' End");
                        return; // 프로세스 종료
                    }
                    // 3-2. 일반 토큰이 만료된 경우는 인증 실패
                    logger.info("JwtAuthenticationFilter doFilterInternal ExpiredJwtException catch End");
                    throw e; // 이후 catch 블록에서 처리됨
                }
                logger.info("JwtAuthenticationFilter doFilterInternal try-catch문 End");

                String subject = claims.getSubject(); // subject는 email 또는 TEMP 또는 RESET 
                logger.info("JwtAuthenticationFilter doFilterInternal subject :" + subject);

                // 5. TEMP || RESET 토큰은 인증 생략하고 통과시킴
                if ("TEMP".equals(subject) || "RESET".equals(subject)) {
                    logger.info("TEMP token validated successfully. No authentication required.");

                    if (jwtUtil.validateToken(token)) {
                        logger.debug("TEMP || RESET token is still valid."); // TEMP 토큰이 아직 유효한 경우 로그만 남김
                    }

                    filterChain.doFilter(request, response); // 인증 없이 통과
                    return;
                }
                logger.info("JwtAuthenticationFilter doFilterInternal 임시토큰 검사 END");
                // 임시 토큰 검사 끝
                // ================================================================================================================

                // ================================================================================================================
                // 로그인 토큰 검사 시작
                // 로그인 토큰 (3시간 짜리 추후 -> 30분으로 변경)
                // 6. 일반 토큰 유효성 검사
                logger.info("JwtAuthenticationFilter doFilterInternal 로그인 토큰 검사 Start");
                if (jwtUtil.validateToken(token)) {
                    String email = jwtUtil.getEmailFromToken(token).trim(); // 이메일 추출

                    // 7. 이메일로 사용자 조회
                    Member member = memberRepository.findByEmail(email)
                            .orElseThrow(() -> {
                            	logger.error("JwtAuthenticationFilter doFilterInternal findByEmail() Error: 등록된 이메일이 없습니다 emil   :"+ email);
                                return new IllegalArgumentException("등록된 이메일이 없습니다.");
                            });

                    // 8. UserDetails(사용자정보) 객체 생성
                    logger.info("JwtAuthenticationFilter doFilterInternal CustomUserDetails 객체 생성");
                    CustomUserDetails userDetails = new CustomUserDetails(member);

                    // 9. 인증(인증 성공) 객체 생성
                    logger.info("JwtAuthenticationFilter doFilterInternal UsernamePasswordAuthenticationToken Start");
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    																							userDetails, // 인증된 사용자 정보(Principal)
                    																							null, // Credentials (비밀번호 등), 여기서는 null
                    																							userDetails.getAuthorities() // 권한 리스트 반드시 넣어야 함 (getAuthorities()에서 반환한 권한)
                    																						    );
                    logger.info("JwtAuthenticationFilter doFilterInternal UsernamePasswordAuthenticationToken End");

                    // 10. 요청 정보와 함께 인증 객체 설정
                    logger.info("JwtAuthenticationFilter doFilterInternal 요청 정보와 함께 인증 객체 설정");
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 11. SecurityContext에 인증 등록
                    // @AuthenticationPrincipal 등으로 인증된 사용자 정보를 참조 가능
                    logger.info("JwtAuthenticationFilter doFilterInternal SecurityContext에 인증 등록, @AuthenticationPrincipal 으로 인증된 사용자 정보를 참조 가능 ");
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
                logger.info("JwtAuthenticationFilter doFilterInternal First 'IF' END");
            }
        } catch (IllegalArgumentException e) {
            // 사용자를 찾을 수 없는 경우
        	logger.error("JwtAuthenticationFilter doFilterInternal IllegalArgumentException Error : 사용자를 찾을 수 없습니다.");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed: user not found.");
            return;
        } catch (ExpiredJwtException e) {
            // 일반 토큰이 만료된 경우
        	logger.error("JwtAuthenticationFilter doFilterInternal ExpiredJwtException Error : 일반 토큰이 만료 되었습니다.");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token has expired.");
            return;
        } catch (Exception e) {
            // 기타 예외
        	logger.error("JwtAuthenticationFilter doFilterInternal Exception Error : 서버 에러");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed.");
            return;
        }

        // 다음 필터로 넘김
        logger.info("JwtAuthenticationFilter doFilterInternal End : 다음 필터로 넘김");
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