package com.example.demo.repository.visitor;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.domain.visitor.VisitorHistory;

public interface VisitorHistoryRepository extends JpaRepository<VisitorHistory, Long> {

	// 일주일, 한달동안 로그인 한 회원 수 세기(조회)
	@Query(
			"SELECT COUNT(DISTINCT COALESCE(v.member.id, v.ipAddress)) "
		  + "  FROM VisitorHistory v "
		  + " WHERE v.visitTime BETWEEN :start AND :end "
		  )
	long countDistinctVisitorsBetween(@Param("start") LocalDateTime start,
			                          @Param("end")LocalDateTime end);

	// visitorId 기준 오래된 기록 삭제 (최근 10건 제외)
	@Modifying // "UPDATE" or "DELETE"
	@Query(value = 
			"DELETE vh "
		   +"  FROM visitor_history vh "
		   +"  JOIN ( "
		   + "       SELECT id "
		   + "         FROM ( "
		   + "               SELECT id,"
		   + "                      ROW_NUMBER() OVER (PARTITION BY visitor_id ORDER BY login_time DESC) AS rn "
		   + "                  FROM visitor_history "
		   + "              ) AS t "
		   + "        WHERE t.rn > 10 "
		   + "      ) AS t2 ON vh.id = t2.id",nativeQuery = true) 
	void deleteOldVisits();

	// 일별 중복 없는 방문자 수
	@Query(
			"SELECT COUNT(DISTINCT v.visitorId) "
		  + "  FROM VisitorHistory v "
		  + " WHERE FUNCTION('YEAR', v.visitTime) = :year"
		  + "   AND FUNCTION('MONTH', v.visitTime) = :month "
		  + "   AND FUNCTION('DAY', v.visitTime) =:day"
		  )
	long countUniqueVisitorsByDate(@Param("year") int year,
			                       @Param("month") int month,
			                       @Param("day") int day);
	// 월별 중복 없는 방문자 수
	@Query(
			"SELECT COUNT(DISTINCT v.visitorId) "
		  + "  FROM VisitorHistory v "
		  + " WHERE FUNCTION('YEAR', v.visitTime) = :year"
		  + "   AND FUNCTION('MONTH', v.visitTime) = :month "
		  )
	long countUniqueVisitorsByMonth(@Param("year") int year,
			                       @Param("month") int month);

	// 'visitorId'기준 VisitorHistory 테이블 전체 조회 (로그인 시 memberId 연결용)
	List<VisitorHistory> findAllByVisitorId(String visitorId);

	
	// 전체 중복 없는 방문자 수(추후에 사용 될 수 있으므로, 보류)
	@Query(
			"SELECT COUNT(DISTINCT v.visitorId) "
		  + "  FROM VisitorHistory  v "
		  )
	long countUniqueVisitors();
}
