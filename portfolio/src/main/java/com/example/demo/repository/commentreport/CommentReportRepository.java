package com.example.demo.repository.commentreport;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.domain.comment.Comment;
import com.example.demo.domain.commentreport.CommentReport;

public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {

	boolean existsByCommentAndReporterId(Comment comment, Long reporterId);

	long countByComment(Comment comment);

}
