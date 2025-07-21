package com.example.demo.domain.postreaction;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.example.demo.domain.post.Post;
import com.example.demo.domain.postImage.PostImage;
import com.example.demo.domain.postreaction.enums.ReactionType;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
	Entity : post_reaction

	Table : post_reaction

	Column
	reaction_id			(리액션 아이디)
	post				(게시글 : ManyToOne)
	userId				(유저 ID)
	reactionType		(좋아요, 싫어요)
	createdAt			(이미지 업로드(생성)일자)
*/

@Entity // 테이블,속성 생성과 매핑을  위한 어노테이션
@Table(
		name = "post_reaction",
		// postId, userId 조합에 대해 중복 반응을 막기 위한,
		// uniqueConstraints
		uniqueConstraints = {
				@UniqueConstraint(columnNames = {"post_id", "user_id"})
		}
)
@Data
@AllArgsConstructor
@NoArgsConstructor
//등록,수정 일자를 자동관리하기 위한 어노테이션
@EntityListeners(AuditingEntityListener.class)
public class PostReaction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "reaction_id")
	private Long reactionId;

	@ManyToOne(fetch = FetchType.LAZY )
	@JoinColumn(name = "post_id", nullable = false)
	private Post post;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "reaction_type", nullable = false)
	private ReactionType reactionType;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

}
