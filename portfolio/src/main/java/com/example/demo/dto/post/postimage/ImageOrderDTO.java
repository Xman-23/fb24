package com.example.demo.dto.post.postimage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
	이미지 정렬 순서 DTO

	Request(요청)
	imageId		(이미지 아이디)
	orderNum	(순서 정렬)
*/

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageOrderDTO {

	private Long imageId;

	private int orderNum;

}
