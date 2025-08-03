package com.example.demo.domain.commentreaction;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.example.demo.domain.comment.Comment;
import com.example.demo.domain.postreaction.postreactionenums.ReactionType;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity : comment_reaction
 *
 * Table : comment_reaction
 *
 * Columns:
 * reaction_id      (PK, 자동 생성 ID)
 * comment_id       (댓글 ID, ManyToOne 관계)
 * user_id          (사용자 ID)
 * reaction_type    (반응 타입, LIKE or DISLIKE)
 * created_at       (생성일자)
 */

@Entity
@Table(
		name = "comment_reaction",
		uniqueConstraints = {
				@UniqueConstraint(columnNames = {"comment_id", "user_id"})
		}
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class CommentReaction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "reaction_id")
	private Long reactionId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "comment_id", nullable = false)
	private Comment comment;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "reaction_type", nullable = false)
	private ReactionType reactionType;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

}
