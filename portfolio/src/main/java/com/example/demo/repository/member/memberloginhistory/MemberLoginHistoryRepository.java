package com.example.demo.repository.member.memberloginhistory;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.member.memberauth.MemberLoginHistory;

@Repository
public interface MemberLoginHistoryRepository extends JpaRepository<MemberLoginHistory, Long> {

	// 일주일, 한달동안 로그인 한 회원 수 세기(조회)
	@Query(
			"SELECT COUNT(DISTINCT lh.member.id) "
		  + "  FROM MemberLoginHistory lh "
		  + " WHERE lh.loginTime BETWEEN :start AND :end "
		  )
	long countDistinctByLoginTimeBetween(@Param("start") LocalDateTime start,
			                             @Param("end")LocalDateTime end);

}
