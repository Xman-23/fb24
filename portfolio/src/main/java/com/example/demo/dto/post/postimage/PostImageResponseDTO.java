package com.example.demo.dto.post.postimage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 	이미지 조회 DTO
 
 	Response(응답)
 	imageId		(이미지 아이디)
 	imageUrl	(이미지 URL)
 	orderNum	(순서 정렬)
 */

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostImageResponseDTO {

	private Long imageId;

	private String imageUrl;

    private String originalFileName;   // 원본 파일명 추가

	private int orderNum;

}
