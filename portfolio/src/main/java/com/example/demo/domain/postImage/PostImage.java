package com.example.demo.domain.postImage;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.example.demo.domain.board.Board;
import com.example.demo.domain.post.Post;
import com.example.demo.domain.post.postenums.PostStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/*
	Entity : post_image
	
	Table : post_image
	
	Column
	image_id			(이미지 아이디)
	post				(게시글 : ManyToOne)
	imageUrl			(이미지 URL)
	order_num			(이미지 순서)
	createdAt			(이미지 업로드(생성)일자)
*/

@Entity //테이블,속성 생성과 매핑을  위한 어노테이션
@Table(name = "post_image")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
//등록,수정 일자를 자동관리하기 위한 어노테이션
@EntityListeners(AuditingEntityListener.class)
public class PostImage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "image_id")
	private Long imageId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "post_id", nullable = false)
	private Post post;

	@Column(name = "image_url", nullable = false, length = 255)
	private String imageUrl;

	@Column(name = "order_num", nullable = false)
	private int orderNum;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

}
