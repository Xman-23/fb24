package com.example.demo.repository.comment;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.comment.Comment;
import com.example.demo.domain.comment.commentenums.CommentStatus;
import com.example.demo.domain.post.Post;

import java.util.*;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

	// 해당 게시글과 모든 상태의 댓글을 가져옴
	List<Comment> findByPostAndStatusIn(Post post, List<CommentStatus> statuses);

	// 댓글 갯수 세기
	int countByPostPostId (Long postId);

	// 게시글들의 모든 댓글 집계 조회
	@Query(
			"SELECT c.post.postId AS postId, "
			+ "     COUNT(c) AS commentCount "
		  + "  FROM Comment c "
		  + " WHERE c.post.postId IN (:postIds) "
		  + " GROUP BY c.post.postId "
		  )
	List<PostCommentCount> countCommentsByPostIds(@Param("postIds") List<Long> postIds);

	interface PostCommentCount {
		Long getPostId();
		Long getCommentCount();
	}

}
