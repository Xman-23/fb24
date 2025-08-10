package com.example.demo.service.comment.commentreaction;

import java.util.List;

import com.example.demo.dto.comment.commentreaction.CommentReactionRequestDTO;
import com.example.demo.dto.comment.commentreaction.CommentReactionResponseDTO;

public interface CommentReactionService {

	// 댓글 리액션  
	CommentReactionResponseDTO reactionToComment (Long commentId,
			                                      Long memberId,
			                                      CommentReactionRequestDTO commentReactionRequestDTO);

	void commentRemoveReactionsByDeadUsers (List<Long> userIds);

}
