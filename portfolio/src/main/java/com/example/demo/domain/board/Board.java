package com.example.demo.domain.board;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.example.demo.domain.comment.Comment;
import com.example.demo.domain.post.Post;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.util.*;

/*
 	Entity : Board

 	Table : Board
 
 	Column
 	boardId		(PK)
 	name		(게시판 제목 ('게시판 제목'으로 '게시판 구분'
 				ex)'자유게시판', '공지게시판', '공유게시판', '정보게시판')
 				
 	description	(게시판 간단설명)
 	posts		(해당게시판에속한 게시판목록)
 	isActive	(게시판 숨김기능)
 	sortOrder	(게시판 순서나열기능)
 	childBoards	('부모 게시판'입장에서 여러개의 '자식 게시판'을 가짐)
 	parentBoard	('자식 게시판'입장에서 '단! 하나만'의 '부모 게시판'을 가짐)
 	createAt	(등록일자)
 	updateAt	(수정일자)
 */

@Entity //테이블,속성 생성과 매핑을  위한 어노테이션
@Table(name = "board")
// List 클래스에 의해 'toString()', 'equals()', 'hashCode'에러 
// 발생할 수 있으므로 '@Data' 권장X
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
// 등록,수정 일자를 자동관리하기 위한 어노테이션
@EntityListeners(AuditingEntityListener.class)
@ToString(onlyExplicitlyIncluded = true) // 연관관계 제외
@EqualsAndHashCode(onlyExplicitlyIncluded = true) // 연관관계 제외
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "board_id")
    private Long boardId;

    @Column(name = "name", nullable = false, length = 100, unique = true)
    private String name;

    @Column(name = "description", nullable= true, length = 255)
    private String description;

    @Column(name = "is_active")
    private boolean isActive = true;

    @Column(name = "sort_order")
    private Integer sortOrder = 0; // 필요하면 추가

    /*
      하나의 게시판'Board'에 여러개의 게시글'Post'를 담는 관계이므로 'OneToMany'
      하나의 Board(게시판)는 여러 개의 Post(게시글)를 가질 수 있음 (1:N 관계)
      mappedBy = "board" → Post 엔티티의 board 필드를 통해 양방향 매핑됨
      즉, Post.board가 외래키를 가지고 있는 '연관관계의 주인'이고,
      Board.posts는 단순히 매핑을 반영하는 '비주인'임
    */
    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Post> posts = new ArrayList<>();

    /* 
      부모 게시판(자유게시판, 공지게시판등) 입장에서의 자식 게시판(유머, 일상) 목록(OneToMany(자기참조 관계))
      예: 자유게시판(부모 게시판) -> 유머, 일상 등.. 자식 게시판(Board)을 가짐
      '@OneToMany': mappedBy 사용 ('DB'에 필드가 만들어지지 않지만, 계층(자식)조회하기 유용함)
    */
    @OneToMany(mappedBy = "parentBoard", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Board> childBoards = new ArrayList<>();

    /* 
      자식 게시판(유머, 일상등) 입장에서의 부모 게시판(자유게시판, 공지게시판) (ManyToOne(자기참조 관계))
      예: 유머, 일상 게시판 -> 자유게시판을 부모로 가짐
      '@ManyToOne': '@JoinColumn' 사용 '외래키의 주인'
    */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name= "parent_id")
    private Board parentBoard;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // '생성일자' 이므로 '업데이트' 불가능

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

}
