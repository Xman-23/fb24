package com.example.demo.dto.comment.commentreaction;

import com.example.demo.domain.post.postreaction.postreactionenums.PostReactionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentReactionResponseDTO {

	private Long commentId;

	private int likeCount;

	private int dislikeCount;

	private PostReactionType userCommentReactionType;

	public static CommentReactionResponseDTO fromEntityToDto(Long commentId, 
			                                                 int likeCount,
			                                                 int dislikeCount,
			                                                 PostReactionType userCommnetReactionType) {

		return CommentReactionResponseDTO.builder()
				                         .commentId(commentId)
				                         .likeCount(likeCount)
				                         .dislikeCount(dislikeCount)
				                         .userCommentReactionType(userCommnetReactionType)
				                         .build();
	}

}
