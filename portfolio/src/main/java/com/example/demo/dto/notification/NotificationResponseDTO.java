package com.example.demo.dto.notification;

import java.time.LocalDateTime;

import com.example.demo.domain.notification.Notification;
import com.example.demo.domain.notification.notificationenums.NotificationType;

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
public class NotificationResponseDTO {

	// 작성자가 받을 알림ID
	private Long notificationId;

	// 작성자가 받을 알림타입
	private NotificationType notificationType;

	// 작성자가 받을 알림 메세지
	private String notificationMessage;

	// 작성자가 작성한 게시글ID
	private Long postId;

	// 작성자가 작성한 댓글ID
	private Long commentId;

	// 읽음 여부
	private boolean read;

	// 알림이 생성된 시간
	private LocalDateTime createdAt;

	// 알림을 보낸 회원의 닉네임
	private String senderNickname;

	public static NotificationResponseDTO fromDto(Notification notification) {
	    return NotificationResponseDTO.builder()
	                                  .notificationId(notification.getNotificationId())
	                                  .notificationType(notification.getNotificationType())
	                                  .notificationMessage(notification.getNotificationMessage())
	                                  .postId(notification.getPostId() != null ? notification.getPostId() : 0L) // 또는 Optional<Long> 처리
	                                  .commentId(notification.getCommentId() != null ? notification.getCommentId() : 0L)
	                                  .read(notification.isRead())
	                                  .createdAt(notification.getCreatedAt())
	                                  .senderNickname(notification.getSenderNickname())
	                                  .build();
	}

}
