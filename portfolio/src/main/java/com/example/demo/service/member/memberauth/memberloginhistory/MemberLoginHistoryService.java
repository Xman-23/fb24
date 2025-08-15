package com.example.demo.service.member.memberauth.memberloginhistory;

public interface MemberLoginHistoryService {

	long getTodayDistinctLogins();

	long getMonthDistinctLogins();

	long getYearDistinctLogins();

}
