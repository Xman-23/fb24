package com.example.demo.controller.member.memberauth.memberloginhistory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.member.memberauth.memberloginhistory.MemberLoginHistoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class MemberLoginHistoryController {

	private final MemberLoginHistoryService memberLoginHistoryService;

	// 로그
	private static final Logger logger = LoggerFactory.getLogger(MemberLoginHistoryController.class);

	//*************************************************** API Start ***************************************************//

	@GetMapping("/today")
	public ResponseEntity<?> getTodayDistinctLogins() {

		logger.info("MemberLoginHistoryController getTodayDistinctLogins() Start");

		long response = memberLoginHistoryService.getTodayDistinctLogins();

		logger.info("MemberLoginHistoryController getTodayDistinctLogins() End");
		return ResponseEntity.ok(response);
	}

	@GetMapping("/month")
	public ResponseEntity<?> getMonthDistinctLogins() {

		logger.info("MemberLoginHistoryController getMonthDistinctLogins() Start");

		long response = memberLoginHistoryService.getMonthDistinctLogins();

		logger.info("MemberLoginHistoryController getMonthDistinctLogins() End");
		return ResponseEntity.ok(response);
	}

	@GetMapping("/year")
	public ResponseEntity<?> getYearDistinctLogins() {

		logger.info("MemberLoginHistoryController getYearDistinctLogins() Start");

		long response = memberLoginHistoryService.getYearDistinctLogins();

		logger.info("MemberLoginHistoryController getYearDistinctLogins() End");
		return ResponseEntity.ok(response);
	}

	//*************************************************** API End ***************************************************//
}
