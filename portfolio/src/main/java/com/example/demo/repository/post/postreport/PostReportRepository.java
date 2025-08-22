package com.example.demo.repository.post.postreport;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.domain.post.Post;
import com.example.demo.domain.post.postreport.PostReport;

public interface PostReportRepository extends JpaRepository<PostReport, Long> {

	@Query(
			"SELECT CASE "
		  + "			WHEN(COUNT(pr)>0) THEN true "
		  + "			ELSE false "
		  + "        END "
		  + "  FROM PostReport pr "
		  + " WHERE pr.post = :post "
		  + "   AND pr.reporterId = :reporterId "
		  )
	boolean existsByPostAndReporterId(@Param("post")Post post, 
								      @Param("reporterId")Long reporterId);

}
