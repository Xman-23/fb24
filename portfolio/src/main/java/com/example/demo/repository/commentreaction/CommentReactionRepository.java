package com.example.demo.repository.commentreaction;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.domain.comment.Comment;
import com.example.demo.domain.commentreaction.CommentReaction;
import com.example.demo.domain.postreaction.enums.ReactionType;

public interface CommentReactionRepository extends JpaRepository<CommentReaction, Long> {

	Optional<CommentReaction> findByCommentAndUserId(Comment comment, Long userId);

	int countByCommentCommentIdAndReactionType(Long commentId, ReactionType reactionType);

	boolean existsByCommentAndUserId(Comment comment, Long userId);

	void deleteByCommentAndUserId(Comment comment, Long userId);

}
