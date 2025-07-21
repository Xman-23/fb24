package com.example.demo.domain.board;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.example.demo.domain.post.Post;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
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
 	sortOred	(게시판 순서나열기능)
 	createAt	(등록일자)
 	updateAt	(수정일자)
 */

@Entity //테이블,속성 생성과 매핑을  위한 어노테이션
@Table(name = "board")
// List 클래스에 의해 'toString()', 'equals()', 'hashCode'에러 
// 발생할 수 있으므로 '@Data' 권장X
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
// 등록,수정 일자를 자동관리하기 위한 어노테이션
@EntityListeners(AuditingEntityListener.class)
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_id")
    private Long boardId;

    @Column(name = "name", nullable = false, length = 100, unique = true)
    private String name;

    @Column(name = "description", nullable= true, length = 255)
    private String description;

    @Column(name = "is_active")
    private boolean isActive = true;

    @Column(name = "sort_order")
    private int sortOrder = 0; // 필요하면 추가

    // 하나의 게시판'Board'에 여러개의 게시글'Post'를 담는 관계이므로 'OneToMany'
    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude // '@Data'로 인해 생기는 'toString()'에 필드 제외(Exclude)
    @EqualsAndHashCode.Exclude // '@Data'로 인해 생기는 'equals()', 'hashcode()'에 필드 제외(Exclude)
    private List<Post> posts;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // '생성일자' 이므로 '업데이트' 불가능

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

}
