package com.example.demo.domain.notification;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.example.demo.domain.member.Member;
import com.example.demo.domain.notification.notificationenums.NotificationType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notification")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

	@Id
	@Column(name = "notification_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long notificationId;

	// 알림받는 사용자
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "receiver_id", nullable = false)
	private Member receiver;

	// 알림 타입
	@Enumerated(EnumType.STRING)
	@Column(name = "notification_type")
	private NotificationType notificationType;

	// 알림 메시지
	@Column(name = "notification_message", nullable = false,length = 500)
	private String notificationMessage;

	// 알림을 보낸 사람
	@Column(name = "sender_nickname", nullable = false)
	private String senderNickname;

	// 작성자가 작성한 해당 게시글ID
	// ex) "회원님의 해당ID 게시글에 댓글, 좋아요가 있습니다."
	@Column(name = "post_id")
	private Long postId;

	// 작성자가 작성한 해당 댓글ID
	// ex) "회원님의 해당ID 댓글에 댓글, 좋아요가 있습니다.
	@Column(name = "comment_id")
	private Long commentId;

	// 읽음 여부
	@Column(name = "is_read", nullable = false)
	private boolean read = false;

	// 소프트 삭제 여부
	@Column(name = "deleted", nullable = false)
	private boolean deleted = false;

	// 생성시간
	@Column(name = "created_at", nullable = false , updatable = false)
	@CreationTimestamp
	private LocalDateTime createdAt;

	// 소프트 삭제를 위한 수정 시간
	@Column(name = "updated_at")
	@UpdateTimestamp
	private LocalDateTime updatedAt;

}
