package com.example.demo.dto.post;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.example.demo.domain.post.Post;
import com.example.demo.domain.post.postenums.PostStatus;
import com.example.demo.domain.post.postimage.PostImage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
	게시글 응답 DTO

	Response	(응답)
	postId			(게시글ID)
	boardId			(게시판ID)
	boardName		(게시판이름)
	title			(게시글 제목)
	content			(게시글 본문 내용)
	authorID		(작성자ID)
	viewCount		(조회수)
	likeCount		(좋아요 수)
	dislikeCount	(싫어요 수)
	isNotice		(공지글 여부)
	status			(게시글 상태)
	createdAt		(게시글 생성일자)
	updatedAt		(게시글 수정일자)
	imageUrls		(이미지 URL)
	commentCount	(댓글 총갯수)
*/

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponseDTO {

	// 필드 Start
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

	private String createdAt;

	private String updatedAt;

	private List<String> imageUrls;

	private String userNickname;

	private boolean pinned;
	// 필드 End

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


	// Entity(엔티티) -> DTO 변환 메서드
	public static PostResponseDTO convertToPostResponseDTO(Post post, 
														   String userNickname,
														   int likeCount,
														   int disLikeCount) {

		List<String> images = Optional.ofNullable(post.getImages())
                                      .orElse(Collections.emptyList())
                                      .stream()
                                      .map(PostImage::getImageUrl)
                                      .collect(Collectors.toList());

    	String formatCreatedAt = DATE_TIME_FORMATTER.format(post.getCreatedAt());
    	String formatUpdatedAt = DATE_TIME_FORMATTER.format(post.getUpdatedAt());
		
		return PostResponseDTO.builder()
				              .postId(post.getPostId())
				              .boardId(post.getBoard().getBoardId())
							  .boardName(post.getBoard().getName())
							  .title(post.getTitle())
							  .content(post.getContent())
							  .authorId(post.getAuthor().getId())
							  .viewCount(post.getViewCount())
							  .likeCount(likeCount)
							  .dislikeCount(disLikeCount)
							  .isNotice(post.isNotice())
							  .status(post.getStatus())
							  .createdAt(formatCreatedAt)
							  .updatedAt(formatUpdatedAt)
							  .imageUrls(images)
							  .userNickname(userNickname)
							  .pinned(post.isPinned())
							  .build();
	}
}
