package com.example.demo.dto.notification;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSummaryResponseDTO {

	// 전체 알림 갯수 (게시글 + 댓글 + 읽지 않은것 포함)
	private long totalCount;

	// 게시글 관련 알림 갯수
	private long postNotificationCount;

	// 댓글 관련 알림 갯수
	private long commentNotificationCount;

	// 최신 게시글 알림 최대 4개
	private List<NotificationResponseDTO> recentPostNotifications;

	// 최신 댓글 알림 최대 4개
	private List<NotificationResponseDTO> recentCommentNotifications;

}
