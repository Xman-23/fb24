package com.example.demo.repository.commentreaction;

import java.util.Optional;
import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.domain.comment.Comment;
import com.example.demo.domain.commentreaction.CommentReaction;
import com.example.demo.domain.postreaction.postreactionenums.ReactionType;

public interface CommentReactionRepository extends JpaRepository<CommentReaction, Long> {

	Optional<CommentReaction> findByCommentAndUserId(Comment comment, Long userId);

	int countByCommentAndReactionType(Comment comment, ReactionType reactionType);

	void deleteByCommentAndUserId(Comment comment, Long userId);
	

	    // 좋아요/싫어요 수를 한번에 매핑하기 위한 내부 인터페이스
	    interface ReactionCountProjection  {
	    	Long getCommentId();
	    	ReactionType getReactionType();
	    	Long getCount();
	    }

	    @Query(" SELECT cr.comment.commentId AS commentId,"
	    	  + "       cr.reactionType AS reactionType, "
	    	  + "       COUNT(cr) AS count "
	    	  + "  FROM CommentReaction cr "
	    	  + " WHERE cr.comment.commentId IN :commentIds "
	    	  + " GROUP BY cr.comment.commentId, cr.reactionType"
	    	  )
	    List<ReactionCountProjection> countReactionsByCommentIds(@Param("commentIds") List<Long> commentIds );

}
