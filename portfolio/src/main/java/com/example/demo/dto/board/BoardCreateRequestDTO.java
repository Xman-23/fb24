package com.example.demo.dto.board;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
	게시판 요청 DTO

	Request
	name		(게시판 제목)
	description	(게시판 설명)
*/

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoardCreateRequestDTO {

	@NotBlank(message = "게시판 이름은 필수입니다.")
	@Size(max = 100, message = "게시판 이름은 100자 이하로 입력해주세요.")
	private String name;

	@Size(max =255, message = "게시판 설명은 255자 이하로 입력해주세요.")
	private String description;

}
