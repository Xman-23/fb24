package com.example.demo.dto.comment.commentreaction;

import com.example.demo.domain.post.postreaction.postreactionenums.PostReactionType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentReactionRequestDTO {

	// 'PostReactionType'으로 'CommentReactionType'으로 사용(좋아요, 싫어요)
	private PostReactionType commentReactionType; 

}
