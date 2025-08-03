package com.example.demo.domain.comment.commentnotification;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.example.demo.domain.comment.Comment;
import com.example.demo.domain.comment.commentnotification.commentnotificationenums.CommentNotificationType;
import com.example.demo.domain.member.Member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import lombok.Data;
import lombok.NoArgsConstructor;

/*
	Entity : notification

	Table : notification

	Column
	notification_id		(알림 아이디, PK)
	receiver_id			(수신자 회원, ManyToOne)
	comment_id			(연관 댓글, ManyToOne)
	content				(알림 내용)
	notification_type	(알림 유형, Enum)
	is_read				(읽음 여부)
	created_at			(생성일자)
*/

@Entity
@Table(name = "comment_notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentNotification {

	@Id
	@Column(name = "notification_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long notificationId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "receiver_id")
	private Member receiver;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "comment_id")
	private Comment comment;

	@Column(name = "content")
	private String content;

	@Enumerated(EnumType.STRING)
	private CommentNotificationType notificationType;

	private boolean isRead = false;

	private LocalDateTime createdAt;

	// DB에 접근할떄 생성일자를 오늘로 설정
	@PrePersist
	public void prePersist() {
		this.createdAt = LocalDateTime.now();
	}

}