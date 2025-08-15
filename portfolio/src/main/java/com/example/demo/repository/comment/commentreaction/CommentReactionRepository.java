package com.example.demo.repository.comment.commentreaction;

import java.util.*;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.domain.comment.Comment;
import com.example.demo.domain.comment.commentreaction.CommentReaction;
import com.example.demo.domain.post.postreaction.postreactionenums.PostReactionType;

public interface CommentReactionRepository extends JpaRepository<CommentReaction, Long> {

	Optional<CommentReaction> findByCommentAndUserId(Comment comment, Long userId);

	int countByCommentAndReactionType(Comment comment, PostReactionType reactionType);

	void deleteByCommentAndUserId(Comment comment, Long userId);
	

	// 댓글 전체 트리 조회
	/** Query 결과
	* commentId | reactionType | count
	* 101       | LIKE         | 2     // 댓글 101에 대한 좋아요 2개
	* 101       | DISLIKE      | 1     // 댓글 101에 대한 싫어요 1개
	* 102       | LIKE         | 1     // (주석 제외, 102번은 무시)
	* 103       | LIKE         | 1     // 댓글 103에 대한 좋아요 1개
	* 103       | DISLIKE      | 1     // 댓글 103에 대한 싫어요 1개
	*/
	@Query( "SELECT cr.comment.commentId AS commentId,"
	      + "       cr.reactionType AS reactionType, "
	      + "       COUNT(cr) AS count "
	      + "  FROM CommentReaction cr "
	      + " WHERE cr.comment.commentId IN (:commentIds) "
	      + " GROUP BY cr.comment.commentId, "
	      + "          cr.reactionType "
	      )
	List<ReactionCountProjection> countReactionsByCommentIds(@Param("commentIds") List<Long> commentIds );

	/* 	
	    countReactionsByCommentIds 쿼리 샐행 결과를 매핑하기 위한 인터페이스,
	 	쿼리 결과에서 각 '댓글ID'별로 리액션 타입(LIKE,DISLIKE)과 각 리액션타입의 갯수를
	 	'get()'으로 가져와 매핑, 'get'으로 가져와야하기때문에 '엔티티의 변수명'과 '별칭명'이 동일해야한다
	 	ex) 'CommentEntity commentId 변수명' == cr.comment.commentId AS 'commentId' 별칭명
	*/
	public interface ReactionCountProjection  {
	    Long getCommentId();
	    PostReactionType getReactionType(); //게시글의 리액션 타입(LIKE, DISLIKE)를 그대로 사용
	    Long getCount();
	}

    // 댓글에 사용할 좋아요, 싫어요 집계조회
    @Query(
    		"SELECT cr.comment.commentId AS commentId, "
    	  + "       COALESCE(SUM(CASE "
    	  + "                         WHEN cr.reactionType = 'LIKE' THEN 1 ELSE 0 "
    	  + "                      END), 0) AS likeCount, "
    	  + "       COALESCE(SUM(CASE "
    	  + "                        WHEN cr.reactionType = 'DISLIKE' THEN 1 ELSE 0"
    	  + "                     END), 0 ) AS dislikeCount "
    	  + "  FROM CommentReaction cr "
    	  + " WHERE cr.comment.commentId = :commentId "
    	  + " GROUP BY cr.comment.commentId "
    	  ) 
    CommentReactionCount countReactionsByCommentId(@Param("commentId") Long commentId);

    public interface CommentReactionCount {
        Long getCommentId();
        Long getLikeCount();
        Long getDislikeCount();
    }

    @Modifying // 'UPDATE'or'DELETE'
    @Query(
    		"DELETE FROM CommentReaction cr "
    	  + " WHERE cr.userId IN (:userIds) "
    	  )
    void deleteAllByUserIdIn(@Param("userIds")List<Long> userIds);

    // 댓글 삭제시 리액션 모두 삭제
    @Modifying // 'UPDATE' or 'DELETE'
    @Query(
    		"DELETE FROM CommentReaction cr "
    	  + " WHERE cr.comment = :comment "
    	  )
    int deleteByComment(@Param("comment") Comment comment);

}
