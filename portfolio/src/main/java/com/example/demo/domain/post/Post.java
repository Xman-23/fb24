package com.example.demo.domain.post;

import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.example.demo.domain.board.Board;
import com.example.demo.domain.comment.Comment;
import com.example.demo.domain.member.Member;
import com.example.demo.domain.post.postenums.PostStatus;
import com.example.demo.domain.post.postimage.PostImage;
import com.example.demo.domain.post.postreaction.PostReaction;
import com.example.demo.domain.post.postreport.PostReport;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/*
	Entity : post

	Table : post

	Column
	postId			(PK)
	board			(게시글이 속한 게시판 : ManyToOne, (유머게시판, 정보게시판, 자유게시판, 공지사항등등)
	title			(게시글 제목)
	content			(게시글 본문)
	authorId		(작성자ID)
	view_count		(조회수 카운트)
	like_count		(좋아요 카운트)
	dislike_count	(싫어요 카운트)
	images			(이미지 첨부(다중) : OneToMany)
	comments		(댓글 목록 : OneToMany)
	reactions		(좋아요/싫어요 기록 : OneToMany))
	created_at		(생성일자)
	updated_at		(수정일자)
	isNotice 		(공지 여부(공지글 인지, 일반글 인지))
	status			(게시글 상태(정상 게시글, 삭제 게시글(물리적 삭제X, 논리적 삭제O(DB에는 남아있음)), 관리자에 의해 차단된 게시글)
*/

@Entity //테이블,속성 생성과 매핑을  위한 어노테이션
@Table(name = "post")
@Setter
@Getter
@Builder
@ToString(exclude = {"images", "comments", "reactions"})
@EqualsAndHashCode(exclude = {"images", "comments", "reactions"})
@AllArgsConstructor
@NoArgsConstructor
//등록,수정 일자를 자동관리하기 위한 어노테이션
@EntityListeners(AuditingEntityListener.class)
public class Post {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "post_id")
	private Long postId;

	// EAGER : 불필요한 JOIN이 많고, 참조 오류가 날 수있음
	// LAZY : 쿼리수 조절 가능, 프록시 방지 (권장)
	@ManyToOne(fetch = FetchType.LAZY) // 여러개의 게시글'Post'에 하나의 게시판'Board'에 속하는 관계이므로 'ManyToOne'
	@JoinColumn(name = "board_id", nullable = false)
	private Board board;

	@Column(name = "title", nullable = false, length = 100)
	private String title;

	@Column(name = "content", columnDefinition = "TEXT", nullable = false)
	private String content;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "author_id", nullable = false)
	private Member author;

	@Column(name = "view_count", nullable = false)
	private int viewCount = 0;

	@Column(name = "like_count", nullable = false)
	private int likeCount = 0;

	@Column(name = "dislike_count", nullable = false)
	private int dislikeCount = 0;

	// 하나의 게시글은 여러개의 이미지를 가질 수 있으므로, @OneToMany
	// 'null' 추가시 'NullPonintException'방지 위해 'ArrayList<>()'로 초기화
	@OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<PostImage> images = new ArrayList<PostImage>();

	// 하나의 게시글은 여러개의 댓글을 가질 수 있으므로, @OneToMany
	// 'null' 추가시 'NullPonintException'방지 위해 'ArrayList<>()'로 초기화
	@OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval =  true)
	private List<Comment> comments = new ArrayList<Comment>();

	// 하나의 게시글은 여러개의 리액션(좋아요, 싫어요)를 가질 수 있으므로, @OneToMany
	// 'null' 추가시 'NullPonintException'방지 위해 'ArrayList<>()'로 초기화
	@OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<PostReaction> reactions = new ArrayList<PostReaction>();

	// 하나의 게시글은 여러개의 신고를 가질 수 있으므로, @OneToMany
	// 'null' 추가시 'NullPonintException'방지 위해 'ArrayList<>()'로 초기화
	@OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<PostReport> reports = new ArrayList<PostReport>();

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;
	
	@Column(name = "is_notice")
	private boolean isNotice = false;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private PostStatus status = PostStatus.ACTIVE;

	@Column(name ="is_pinned")
	private boolean isPinned = false;

	// 추후에 'nullable = false'로 변경해야됨
	@Column(name = "report_count", nullable = true)
	private Long reportCount = 0L;

    // 신고 누적 카운트 증가 메서드
    public void incrementReportCount() {
        if(this.reportCount == null ) {
            this.reportCount = 0L;
        }
        this.reportCount = this.getReportCount() +1L;
    }

	public void addImage(PostImage image) {
	    this.images.add(image);
	    image.setPost(this); // 양방향 동시 설정
	}

}
