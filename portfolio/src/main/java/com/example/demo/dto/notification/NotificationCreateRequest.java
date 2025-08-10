package com.example.demo.dto.notification;

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
public class NotificationCreateRequest {

	// 알림 받는 사람의 memberId;
	private Long receiverId;

	// 알림 타입
	private NotificationType notificationType;

	// 알림을 보낸 사람의 닉네임
	private String senderNickname;

	// 알림을 보낸 사람의 닉네임과 함께 작성될 메세지
	private String notificationMessage;

	// 작성자가 작성한 게시글ID
	private Long postId;

	// 작성자가 작성한 댓글 ID
	private Long commentId;

}
