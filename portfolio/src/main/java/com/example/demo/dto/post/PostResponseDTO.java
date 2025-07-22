package com.example.demo.dto.post;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.example.demo.domain.post.Post;
import com.example.demo.domain.post.postenums.PostStatus;
import com.example.demo.domain.postImage.PostImage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
	게시판 응답 DTO

Response	(응답)
boardId		(게시판ID)
name		(게시판 제목)
description	(게시판 설명)
isActive	(게시판 숨김기능)
sortOrder 	(게시판 순서나열기능)
createdAt	(등록일자)
updatedAt	(수정일자)
*/

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponseDTO {

	// 수정, 삭제, 정렬그리고 댓글,이미지등 자식 엔티티를 위한 고유식별자(ID)  
	private Long postId;

	private Long boardId;

	private String boardName;

	private String title;

	private String content;

	private Long authorId;

	private int viewCount;

	private int likeCount;

	private int dislikeCount;

	private boolean isNotice;

	private PostStatus status;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

	private List<String> imageUrls;

	private int commentCount;

	private int reactionCount;

	// Entity(엔티티) -> DTO 변환 메서드
	public static PostResponseDTO convertToPostResponseDTO(Post post, int commentCount, int reactionCount) {

		List<String> images = post.getImages() // List<PostImage>
                				  .stream() // Stream<PostImage>
                				  // 'image'는 'PostImage'의 '참조변수' -> 'PostImage.getImageUrl()' 의해서,
                				  // Stream<PostImage> -> Stream<String>
                				  .map(image -> image.getImageUrl()) 
                				  .collect(Collectors.toList()); // Stream<String> -> List<String>


		
		return PostResponseDTO.builder()
				              .postId(post.getPostId())
				              .boardId(post.getBoard().getBoardId())
							  .boardName(post.getBoard().getName())
							  .title(post.getTitle())
							  .content(post.getContent())
							  .authorId(post.getAuthorId())
							  .viewCount(post.getViewCount())
							  .likeCount(post.getLikeCount())
							  .dislikeCount(post.getDislikeCount())
							  .isNotice(post.isNotice())
							  .status(post.getStatus())
							  .createdAt(post.getCreatedAt())
							  .updatedAt(post.getUpdatedAt())
							  .imageUrls(images)
							  .commentCount(commentCount)
							  .reactionCount(reactionCount)
							  .build();
	}
}
