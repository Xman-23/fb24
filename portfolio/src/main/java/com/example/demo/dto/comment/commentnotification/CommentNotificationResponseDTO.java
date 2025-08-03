package com.example.demo.dto.comment.commentnotification;

import java.time.LocalDateTime;

import com.example.demo.domain.comment.commentnotification.CommentNotification;
import com.example.demo.domain.comment.commentnotification.commentnotificationenums.CommentNotificationType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentNotificationResponseDTO {

	// 고유 식별자
    private Long notificationId;

    // 댓글 내용 or 신고 사유
    private String content;

    // 작성한 댓글로 이동하기 위한 ID
    private Long commentId;

    // 알림 타입
    private CommentNotificationType notificationType;

    // 읽음 상태 표시
    private Boolean isRead;

    // 생성 일자
    private LocalDateTime createdAt;

    public static CommentNotificationResponseDTO toDto(CommentNotification notification) {
    	return CommentNotificationResponseDTO.builder()
    			                      .notificationId(notification.getNotificationId())
    			                      .content(notification.getComment() == null ? "": notification.getComment().getContent())
    			                      .commentId(notification.getComment() == null ? null : notification.getComment().getCommentId())
    			                      .notificationType(notification.getNotificationType())
    			                      .isRead(notification.isRead())
    			                      .createdAt(notification.getCreatedAt())
    			                      .build();
    }

}
