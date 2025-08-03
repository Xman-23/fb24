package com.example.demo.service.comment.commentnotification;

import org.springframework.data.domain.Pageable;

import com.example.demo.domain.comment.commentnotification.commentnotificationenums.CommentNotificationType;
import com.example.demo.dto.comment.commentnotification.CommentNotificationListResponseDTO;

public interface NotificationService {

	// 알림 목록 조회(페이징)
	CommentNotificationListResponseDTO getNotifications(Long memberId, Pageable pageable);

	// 알림 읽음 처리
	void markAsRead(Long notificationId, Long memberId);

	// 읽지 않은 알림 개수 조회
	long countUnreadNotifications(Long memberId);

	void createNotification (Long receiverId, Long commentId, CommentNotificationType type, String content);

}
