package com.example.demo.controller.visitor;

import java.time.LocalDate;


import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.jwt.JwtService;
import com.example.demo.service.visitor.VisitorHistoryService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/visitors")
public class VisitorHistoryController {

	private final JwtService jwtService; // JWT 토큰 검증/정보 추출 서비스
    private final VisitorHistoryService visitorHistoryService;

    private static final Logger logger = LoggerFactory.getLogger(VisitorHistoryController.class);

	//*************************************************** API START ***************************************************//

    // 사이트 접속 시 호출 (쿠키 생성 + 방문 기록)
    @GetMapping("/track")
    public ResponseEntity<?> trackVisit(HttpServletRequest request,
            							HttpServletResponse response,
            							@RequestHeader(value = "Authorization", required = false) String authHeader) {

    	logger.info("VisitorHistoryController trackVisit() Start");

        Long memberId = null;

        // JWT가 있는 경우(회원) 'memberId' 추출, 없는 경우, 비회원이므로
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
        	// "Bearer " 다음부터 '문자'잘라냄
            String token = authHeader.substring(7);
            try {
            	memberId = jwtService.getMemberIdFromToken(token);				
			} catch (NoSuchElementException e) {
				logger.error("VisitorHistoryController () NoSuchElementException : {}",e.getMessage(),e);
				ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
			}
            
        }

        visitorHistoryService.trackVisit(request, response, memberId);

        logger.info("VisitorHistoryController trackVisit() End");
        return ResponseEntity.noContent().build();
    }

    // 특정 날짜 하루 방문자 수 조회
    @GetMapping("/daily")
    public ResponseEntity<?> getDailyVisitors(@RequestParam(name = "date") String date) {

    	logger.info("VisitorHistoryController getDailyVisitors() Start");

    	LocalDate localDate = LocalDate.parse(date);

        long response = visitorHistoryService.getDailyUniqueVisitors(localDate);

        logger.info("VisitorHistoryController getDailyVisitors() Start");

        return ResponseEntity.ok(response);
    }

    // 특정 월 방문자 수 조회
    @GetMapping("/monthly")
    public ResponseEntity<?> getMonthlyVisitors(@RequestParam(name = "yearMonth") String yearMonth) {

    	logger.info("VisitorHistoryController getMonthlyVisitors() Start");

    	LocalDate localDate = LocalDate.parse(yearMonth); 

        long response = visitorHistoryService.getMonthlyUniqueVisitors(localDate);

        logger.info("VisitorHistoryController getMonthlyVisitors() End");

        return ResponseEntity.ok(response);
    }

	//*************************************************** API End ***************************************************//
}