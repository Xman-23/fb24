package com.example.demo.dto.post;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
	게시글 수정 DTO

	Request(요청)
	title		(게시글 게목)
	content		(게시글 내용)
	isNotice 	(공지 여부(공지글 인지, 일반글 인지))
	images		(이미지'들')
*/

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostUpdateRequestDTO {

	// 문자는 "NotBlank"
	@NotBlank(message = "게시글 제목은 필수입니다.")
	@Size(min = 2 , max = 100, message = "제목은 2글자 이상, 100자 이하로 입력해주세요.")
	private String title;
	
	// 숫자는 "NotNull"
	@NotNull(message = "게시판 ID는 필수입니다.")
	private Long boardId; // 게시판 ID

	@NotBlank(message = "게시글 내용은 필수입니다.")
	private String content;

	private boolean notice = false;

	private List<MultipartFile> images;

}