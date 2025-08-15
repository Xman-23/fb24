package com.example.demo.service.member.memberauth.memberloginhistory;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.repository.member.memberloginhistory.MemberLoginHistoryRepository;
import com.example.demo.service.member.memberauth.MemberAuthService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
class MemberLoginHistoryServicelmpl implements MemberLoginHistoryService{

	private final MemberLoginHistoryRepository memberLoginHistoryRepository;

	// 로그
	private static final Logger logger = LoggerFactory.getLogger(MemberLoginHistoryServicelmpl.class);

	//*************************************************** Service START ***************************************************//

	// 오늘 접속한 회원 세기
	public long getTodayDistinctLogins() {

		logger.info("MemberLoginHistoryService getTodayUniqueLogins() Start");

		// 'loginStart'기준을 오늘(ex 2025-08-15) 자정(00시00분00초)으로 맞춤 
		LocalDateTime loginStart = LocalDate.now().atStartOfDay();
		// 'loginStart(오늘 자정)'을 기준으로 내일 자정까지를 'loginEnd'로 맞춤
		// ex) 2025-08-16 00시 00분 00초
		LocalDateTime loginEnd = loginStart.plusDays(1);

		// 하루 동안 로그인한 '회원들'을 중복없이 조회
		long response = memberLoginHistoryRepository.countDistinctByLoginTimeBetween(loginStart, loginEnd);

		logger.info("MemberLoginHistoryService getTodayUniqueLogins() End");
		return response;
	}

	// 한달동안 접속한 회원 세기
	public long getMonthDistinctLogins() {

		logger.info("MemberLoginHistoryService getMonthDistinctLogins() Start");

		// 'loginStart'기준을 현재날짜의 '달'의'1(withDayOfMonth(1))'일 기준
		// ex) 08-15 -> 08-01 00시 00분 00초
		LocalDateTime loginStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
		// 'loginEnd'기준을 'loginStart(이번달 '1일')을 기준으로 다음달의 1일을 기준
		// ex) 08-15 -> 09-01
		LocalDateTime loginEnd = loginStart.plusMonths(1).toLocalDate().withDayOfMonth(1).atStartOfDay();

		// 한달 동안 로그인한 '회원들'을 중복없이 조회
		long response = memberLoginHistoryRepository.countDistinctByLoginTimeBetween(loginStart, loginEnd);

		logger.info("MemberLoginHistoryService getMonthDistinctLogins() End");
		return response;
	}

	// 일년동안 접속한 회원수 세기
	public long getYearDistinctLogins() {

		logger.info("MemberLoginHistoryService getYearDistinctLogins() Start");

		// 'loginStart'기준을 현재날짜의 '년'의'1'일 기준
		// ex 2025-08-15 -> 2025-01-01 00시 00분 00초
		LocalDateTime loginStart = LocalDate.now().withDayOfYear(1).atStartOfDay();
		// 'loginEnd'기준을 'loginStart(이번년도 '01월01일')을 기준으로 내년 01월 01일을 기준 
		LocalDateTime loginEnd = LocalDate.now().plusYears(1).withDayOfYear(1).atStartOfDay();

		long response =memberLoginHistoryRepository.countDistinctByLoginTimeBetween(loginStart, loginEnd);

		logger.info("MemberLoginHistoryService getYearDistinctLogins() End");
		return response;
	}

	//*************************************************** Service End ***************************************************//

}
