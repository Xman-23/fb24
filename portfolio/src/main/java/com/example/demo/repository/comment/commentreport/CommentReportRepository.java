package com.example.demo.repository.comment.commentreport;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.domain.comment.Comment;
import com.example.demo.domain.comment.commentreport.CommentReport;

public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {

	@Query(
			"SELECT CASE "
		  + "			WHEN(COUNT(cr)>0) THEN true "
		  + "			ELSE false "
		  + "        END "
		  + "  FROM CommentReport cr "
		  + " WHERE cr.comment = :comment "
		  + "   AND cr.reporterId = :reporterId "
		  )
	boolean existsByCommentAndReporterId(@Param("comment")Comment comment, 
										 @Param("reporterId")Long reporterId);

}
