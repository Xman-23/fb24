package com.example.demo.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO: 댓글 요청 데이터 전송 객체
 *
 * 필드 설명:
 * postId           - 댓글이 속한 게시글 ID
 * parentCommentId  - 대댓글인 경우 부모 댓글 ID (댓글이면 null)
 * content          - 댓글 내용
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequestDTO {

	@NotNull(message = "게시글ID는 필수입니다.")
	private Long postId; 

	private Long parentCommentId;

	@NotBlank(message =  "댓글 내용은 필수 입니다.")
	@Size(max = 500, message = "댓글 내용은 500글자 이하로 작성해야합니다.")
	private String content;

}
