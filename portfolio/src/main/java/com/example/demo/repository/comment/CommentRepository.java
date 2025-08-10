package com.example.demo.repository.comment;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.comment.Comment;
import com.example.demo.domain.comment.commentenums.CommentStatus;
import com.example.demo.domain.post.Post;

import java.util.*;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

	List<Comment> findByPostAndStatusIn(Post post, List<CommentStatus> statuses);

	int countByPostPostId (Long postId);

}	
