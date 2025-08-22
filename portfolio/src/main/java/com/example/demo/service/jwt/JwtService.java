package com.example.demo.service.jwt;

import java.util.NoSuchElementException;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.demo.jwt.JwtUtil;
import com.example.demo.repository.member.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtUtil jwtUtil;

    private final MemberRepository memberRepository;

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    // 토큰에서 이메일(subject) 추출 후 DB 조회로 memberId 반환
    public Long getMemberIdFromToken(String token) {

    	logger.info("JwtService getMemberIdFromToken() Start");

        String email = jwtUtil.getEmailFromToken(token);

        Long response = memberRepository.findByEmail(email)
        		                          .map(member -> member.getId())
        		                          .orElseThrow(() ->{
        		                        	logger.error("JwtService getMemberIdFromToken NoSuchElementException email : {} ", email);
        		                        	return new NoSuchElementException("회원이 존재 하지 않습니다.");  
        		                          });

        logger.info("JwtService getMemberIdFromToken() End");
        return response;
    }

    // 토큰 유효성 검증
    public boolean validateToken(String token) {
    	logger.info("JwtService getMemberIdFromToken() Start");
    	boolean response = jwtUtil.validateAccessToken(token);
    	logger.info("JwtService getMemberIdFromToken() Start");
        return response;
    }
}
