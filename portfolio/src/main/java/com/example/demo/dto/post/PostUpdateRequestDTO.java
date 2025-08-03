package com.example.demo.dto.post;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

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

	private String title;

	private String content;

	private boolean notice = false;

	private List<MultipartFile> images;

}
