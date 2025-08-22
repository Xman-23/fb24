package com.example.demo.batchscheduler.visitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.demo.service.visitor.VisitorHistoryService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class VisitorScheduler {

	private final VisitorHistoryService visitorHistoryService;

	private static final Logger logger = LoggerFactory.getLogger(VisitorScheduler.class);

	@Scheduled(cron = "0 0 8 * * *")
	public void runDailyCleanup ( ) {

		logger.info("VisitorScheduler runDailyCleanup() Start");
		visitorHistoryService.cleanOldVisits();
		logger.info("VisitorScheduler runDailyCleanup() End");
	}
}
