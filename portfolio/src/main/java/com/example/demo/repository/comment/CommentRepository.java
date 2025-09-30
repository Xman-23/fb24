package com.example.demo.repository.comment;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.comment.Comment;
import com.example.demo.domain.comment.commentenums.CommentStatus;
import com.example.demo.domain.post.Post;
import com.example.demo.domain.post.postenums.PostStatus;

import java.util.*;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query(
    		"SELECT c "
    	  + "  FROM Comment c "
    	  + " WHERE c.member.id = :authorId "
    	  + "   AND c.status = :status "
    	  + " ORDER BY c.createdAt DESC "
    	  )
    Page<Comment> findByAuthor(@Param("authorId") Long authorId,
            				   @Param("status") CommentStatus status,
            				   Pageable pageable);
	
    @Query(
    		"SELECT c "
    	  + "  FROM Comment c "
          + " WHERE c.post = :post "
          + "   AND c.status IN :statuses " 
          + " ORDER BY c.createdAt DESC"
          )
     List<Comment> findByPostWithStatusesDesc(@Param("post") Post post,
                                              @Param("statuses") List<CommentStatus> statuses);
    
    @Query("SELECT c FROM Comment c " +
            "WHERE c.post.postId = :postId " +
            "AND c.parentComment IS NULL ")
     List<Comment> findRootCommentsByPost(@Param("postId") Long postId);

	// 댓글 갯수 세기
	@Query(
			"SELECT COUNT(c) "
		  + "  FROM Comment c "
		  + " WHERE c.post.postId = :postId "
		  + "   AND c.status = :status "
		  )
	int countActiveCommentsByPostId (@Param("postId") Long postId,
			                         @Param("status") CommentStatus status);

	// 게시글들의 모든 댓글 집계 조회
	@Query(
			"SELECT c.post.postId AS postId, "
			+ "     COUNT(c) AS commentCount "
		  + "  FROM Comment c "
		  + " WHERE c.post.postId IN (:postIds) "
		  + "   AND c.status = :status "
		  + " GROUP BY c.post.postId "
		  )
	List<PostCommentCount> countCommentsByPostIds(@Param("postIds") List<Long> postIds,
			                                      @Param("status") CommentStatus status);

	interface PostCommentCount {
		Long getPostId();
		Long getCommentCount();
	}

}
