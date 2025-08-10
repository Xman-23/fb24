package com.example.demo.dto.post.postreaction;

import com.example.demo.domain.post.postreaction.postreactionenums.PostReactionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostReactionResponseDTO {

	private Long postId;

	private int likeCount;

	private int dislikeCount;

	private PostReactionType userPostReactionType;

	public static PostReactionResponseDTO fromEntityToDto(Long postId, 
			                                              int likeCounter, 
			                                              int dislikeCount,
			                                              PostReactionType userPostReactionType ) {

		return PostReactionResponseDTO.builder()
				                      .postId(postId)
				                      .likeCount(likeCounter)
				                      .dislikeCount(dislikeCount)
				                      .userPostReactionType(userPostReactionType)
				                      .build();
	}

}
