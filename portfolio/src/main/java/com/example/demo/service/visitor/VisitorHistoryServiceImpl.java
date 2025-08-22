package com.example.demo.service.visitor;

import java.time.LocalDate;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.domain.member.Member;
import com.example.demo.domain.visitor.VisitorHistory;
import com.example.demo.repository.member.MemberRepository;
import com.example.demo.repository.visitor.VisitorHistoryRepository;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VisitorHistoryServiceImpl implements VisitorHistoryService {

	private final MemberRepository memberRepository;
	private final VisitorHistoryRepository visitorHistoryRepository;

	private static final Logger logger = LoggerFactory.getLogger(VisitorHistoryServiceImpl.class);

	////*************************************************** Method Start ***************************************************//
	// 포록시 헤더
    private String resolveClientIp(HttpServletRequest request) {

    	String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        } else {
            // X-Forwarded-For 헤더에는 "clientIP, proxy1, proxy2" 형식일 수 있어서 첫 번째만 가져오기
            ip = ip.split(",")[0].trim();
        }

    	return ip;
    }

    // 쿠키
    public void trackVisit(HttpServletRequest request, HttpServletResponse response, Long memberId) {
 
        String visitorId = getOrCreateVisitorId(request, response);

        createVisitorHistory(visitorId, request, memberId);
    }


	// 쿠키 또는 세션에서 visitorId 가져오기/ 없으면 생성
    // 쿠키가 생성되는 경우의수 1. 처음방문, 2. 쿠키 만료 후 재방분, 3.사용자가 쿠키를 삭제한 경우
    // Visitor ID 가져오거나 생성
    private String getOrCreateVisitorId(HttpServletRequest request, HttpServletResponse response) {

    	// HttpServletRequest 헤더에 쿠키 여부 확인을 위한 변수
        Cookie[] cookies = request.getCookies();

        // 기존 쿠키가 있다면 기존 "newVisitorId" 리턴
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("VISITOR_ID".equals(c.getName())) {
                    return c.getValue();
                }
            }
        }

        // 없으면 새로 발급
        String newVisitorId = UUID.randomUUID().toString();
        Cookie cookie = new Cookie("VISITOR_ID", newVisitorId);

        // 쿠키 생명주기 3개월
        cookie.setMaxAge(60 * 60 * 24 * 90); 
        // JavaScript 쿠키 접근 방지
        cookie.setHttpOnly(true);
        // HTTPS에서만 쿠키 전송, 지금은 HTTP 개발환경이므로 'false'
        // 추후에 HTTPS로 변경 시 'true'
        cookie.setSecure(false);
        // 방문 접속자 기록을 위해서 'main'만 사용
        // 전역("/")으로 할시 쿠키가 계속 따라오므로, 
        // 보안 or 네트워크 트래픽 방지
        cookie.setPath("/");
        // 동일 사이트 도메인('localhost'),포스트(:8080)만 사용하므로,
        // "SameSite" "LAX"사용
        cookie.setAttribute("SameSite", "Lax");

        // 'response(응답)' 헤더에 설정된 쿠키 반환
        response.addCookie(cookie);
        return newVisitorId;
    }
    //*************************************************** Method End ***************************************************//

	//*************************************************** Service START ***************************************************//	

    // 방문 기록 생성 (일반 방문용)
    @Override
    @Transactional
    public void createVisitorHistory(String visitorId, HttpServletRequest request, Long memberId) {

    	logger.info("VisitorHistoryServiceImpl createVisitorHistory() Start ");

    	// 회원이면 'member'값 셋팅, 비회원이면 null 값 셋팅
    	Member member = null;
    	if(memberId != null) {
    		member = memberRepository.getReferenceById(memberId);
    	}

    	// 쿠키의 생명(3개월)이 있는동안에는 '동일한 visitorId'을 
    	// 현재시간(LocalDateTime.now())을 기준으로 DB에 레코드 추가
        VisitorHistory history = VisitorHistory.builder()
                .visitorId(visitorId)
                .member(member)
                .ipAddress(resolveClientIp(request))
                .userAgent(request.getHeader("User-Agent"))
                .visitTime(LocalDateTime.now())
                .loginTime(member == null ? null : LocalDateTime.now())
                .build();

        visitorHistoryRepository.save(history);

    	logger.info("VisitorHistoryServiceImpl createVisitorHistory() End ");
    }

	// 오래된 방문 기록삭제 (최신 10건 빼고 삭재)
	@Override
	@Transactional
	public void cleanOldVisits() {
		logger.info("VisitServiceImpl getRecentVisits Start");
		
		visitorHistoryRepository.deleteOldVisits();

		logger.info("VisitServiceImpl getRecentVisits End");
	}

	@Override
	@Transactional(readOnly = true)
	public long getDailyUniqueVisitors(LocalDate date) {

		logger.info("VisitServiceImpl getDailyUniqueVisitors() Start");

		/*
			ZoneId zone = ZoneId.of("Asia/Seoul");
			그날의 자정(00 00 00)
			ex) 08 16 17 46 00 - > 08 16 00 00 00
			LocalDateTime start = date.atStartOfDay(zone).toLocalDateTime();
			'start'를 기준으로 하루 추가
			LocalDateTime end = start.plusDays(1);
		*/

		logger.info("VisitServiceImpl getDailyUniqueVisitors() date.getYear(), date.getMonthValue(), date.getDayOfMonth() : {} {} {} ",date.getYear(), date.getMonthValue(), date.getDayOfMonth());

		long response = visitorHistoryRepository.countUniqueVisitorsByDate(date.getYear(), 
				                                                           date.getMonthValue(),
				                                                           date.getDayOfMonth());

		logger.info("VisitServiceImpl getDailyUniqueVisitors() End");
		return response;
	}

	@Override
	@Transactional(readOnly = true)
	public long getMonthlyUniqueVisitors(LocalDate yearMonth) {

		logger.info("VisitServiceImpl getMonthlyUniqueVisitors Start");

		/*
			그달의 1일 자정
			08 16 17 46 00 - > 08 01 00 00 00 
			LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
			그달의 말일 +1
			08 16 17 46 00 -> 08 31 -> 09 01 00 00 00 
			LocalDateTime end = yearMonth.atEndOfMonth().plusDays(1).atStartOfDay();
		*/

		logger.info("VisitServiceImpl getMonthlyUniqueVisitors() yearMonth.getYear(), yearMonth.getMonthValue() : {} {} ", yearMonth.getYear(), yearMonth.getMonthValue());
		long response = visitorHistoryRepository.countUniqueVisitorsByMonth(yearMonth.getYear(), 
				                                                            yearMonth.getMonthValue());

		logger.info("VisitServiceImpl getMonthlyUniqueVisitors Start");
		return response;
	}

	//*************************************************** Service End ***************************************************//	


}
