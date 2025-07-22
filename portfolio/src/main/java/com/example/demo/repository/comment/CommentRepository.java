package com.example.demo.repository.comment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.comment.Comment;
import com.example.demo.domain.post.Post;

import java.util.*;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

	// 특정 게시글의 댓글 전체 조회(대댓글 포함)
	List<Comment> findByPost(Post post);

	// 특정 게시글의 댓글을 페이징 처리하여 조회 (부모 댓글만 조회할 때도 활용 가능)
	// IsNull = 부모댓글, IsNotNull = 대댓글
	Page<Comment> findByPostAndParentCommentIsNull (Post post, Pageable pageable);

	// 특정 부모 댓글의 대댓글 조회
	List<Comment> findByParentComment(Comment parentComment);

	// 댓글 개수 조회
	int countByPostId (Long postId);
	
}
