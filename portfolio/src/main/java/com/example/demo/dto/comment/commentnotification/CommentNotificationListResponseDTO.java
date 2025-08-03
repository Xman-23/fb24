package com.example.demo.dto.comment.commentnotification;

import java.util.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentNotificationListResponseDTO {

	// 알림 목록을 보여주기 위한 List
	private List<CommentNotificationResponseDTO> notifications;

	// 현재 페이지 Number
	private int pageNumber;

	// 한 페이지당 보여줄 데이터 Size
	private int pageSize;

	// 알림 총 데이터 갯수
	private long totalElements;

	// 총 페이지
	private int totalPages;
	
}
