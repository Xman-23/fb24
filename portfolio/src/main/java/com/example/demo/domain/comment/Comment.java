package com.example.demo.domain.comment;

import java.time.LocalDateTime;

import java.util.*;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.example.demo.domain.comment.commentenums.CommentStatus;
import com.example.demo.domain.comment.commentreaction.CommentReaction;
import com.example.demo.domain.comment.commentreport.CommentReport;
import com.example.demo.domain.member.Member;
import com.example.demo.domain.post.Post;

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
	Entity : comment

	Table : comment

	Column
	image_id			(이미지 아이디)
	post				(게시글 : ManyToOne)
	imageUrl			(이미지 URL)
	order_num			(이미지 순서)
	createdAt			(이미지 업로드(생성)일자)
	isPinned			(고정 여부)
	
*/

@Entity //테이블,속성 생성과 매핑을  위한 어노테이션
@Table(name = "comment")
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
//등록,수정 일자를 자동관리하기 위한 어노테이션
@EntityListeners(AuditingEntityListener.class)
@ToString(onlyExplicitlyIncluded = true) // 연관관계 제외
@EqualsAndHashCode(onlyExplicitlyIncluded = true) // 연관관계 제외
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "comment_id")
    private Long commentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    /* 
    	자식 댓글(대댓글) 입장에서의 부모 댓글(댓글) (ManyToOne(자기참조 관계))
    	자식 댓글(대댓글) 입장에서 변수명(부모 변수명)을 정해야함
    	'@ManyToOne': '@JoinColumn' 사용 '외래키의 주인'
    */
    // FetchType.LAZY로 해야 대댓글 조회를 할떄 무한재귀에 빠지지 않아, 스택 오버플로우를 방지할 수 있다
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    // 'parentComment'가 'Null'이면 댓글 , 'notNull'이면 대댓글
    private Comment parentComment;

    /*
    	부모 댓글(댓글) 입장에서의 자식 댓글(대댓글) 목록(OneToMany(자기참조 관계))
    	부모 댓글(댓글) 입장에서 변수명(자식 변수명)을 정해야함
    '	@OneToMany': mappedBy 사용 ('DB'에 필드가 만들어지지 않지만, 계층(자식)조회하기 유용함)
     */
    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<Comment> childComments = new ArrayList<>();

    // 'NullPointerException'을 방지하기 위해 ArrayList<>() 객체 생성(new)
    /*
      	댓글 입장에서는 여러개의 좋아요/싫어요를 가질 수 있으므로 (ManyToOne(1:N관계))
      	@OneToMany : mappedBy 사용 ('DB'에 필드가 만들어지지 않지만, 계층(자식)조회에 유용함)
     */
    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<CommentReaction> reactions = new ArrayList<>();

    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<CommentReport> reports = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private Member member;

    // 추후에 nullable = false로 변경
    @Column(name = "author_nickname", nullable = true)
    private String authorNickname;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

	@Column(name ="is_pinned")
	private boolean isPinned = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CommentStatus status = CommentStatus.ACTIVE; // 기본값은 ACTIVE

    @Column(name = "report_count", nullable = false)
    private Long reportCount = 0L;

    // 신고 누적 카운트 증가 메서드
    public void incrementReportCount() {
        if(this.reportCount == null ) {
            this.reportCount = 0L;
        }
        this.reportCount = this.getReportCount() +1L;
    }

    // 상태 변경 메서드 
    public void setStatus(CommentStatus status) {
        this.status = status;
    }

}
