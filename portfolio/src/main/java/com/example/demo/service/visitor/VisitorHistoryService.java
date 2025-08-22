package com.example.demo.service.visitor;

import java.time.LocalDate;
import java.time.YearMonth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface VisitorHistoryService {

	void trackVisit(HttpServletRequest request, HttpServletResponse response, Long memberId);

	void createVisitorHistory(String visitorId, HttpServletRequest request, Long memberId);

	void cleanOldVisits();

	long getDailyUniqueVisitors(LocalDate date);

	long getMonthlyUniqueVisitors(LocalDate yearMonth);

	//long getUniqueVisitorCount();
}
