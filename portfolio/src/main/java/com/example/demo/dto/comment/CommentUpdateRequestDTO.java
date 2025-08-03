package com.example.demo.dto.comment;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO: 댓글 수정 요청 데이터 전송 객체
 *
 * 필드 설명:
 * content - 수정할 댓글 내용
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentUpdateRequestDTO {

	@NotBlank(message = "댓글 내용은 필수입니다.")
	private String content;

}
