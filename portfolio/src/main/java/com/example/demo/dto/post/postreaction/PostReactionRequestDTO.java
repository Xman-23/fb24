package com.example.demo.dto.post.postreaction;

import com.example.demo.domain.post.postreaction.postreactionenums.PostReactionType;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostReactionRequestDTO {

	@NotNull(message = "반응 타입은 필수입니다.")
	private PostReactionType reactionType;

}
