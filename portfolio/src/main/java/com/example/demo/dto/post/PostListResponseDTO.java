package com.example.demo.dto.post;



import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.demo.domain.post.Post;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
	게시글 목록 응답 DTO

	Response(응답)
	postId			(게시글ID)
	title			(게시글 게목)
	boardName		('게시글'이 속한 '게시판'이름
	viewCount		(조회수)
	likeCount		(좋아요 수)
	dislikeCount	(싫어요 수)
	isNotice		(공지여부)
	createdAt		(게시글 생성일자)
*/

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostListResponseDTO {

	
	private static final Logger logger = LoggerFactory.getLogger(PostListResponseDTO.class);
	// 필드 Start
    private Long postId;

    private Long boardId;

    private String title;

    private String boardName;

    private int viewCount;


    @JsonProperty("notice")
    private boolean isNotice;

    private String createdAt;

    private int reactionCount;

    private String userNickname;

    private int commentCount;

    private String thumbnailImageUrl;
    // 필드 End

    private static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

 
    // 공지용 (댓글 X)
    public static PostListResponseDTO fromEntity(Post post, int reactionCount, String userNickname) {

    	String formatCreatedAt = DATE_TIME_FORMATTER.format(post.getCreatedAt());

        return PostListResponseDTO.builder()
                                  .postId(post.getPostId())
                                  .title(post.getTitle())
                                  .boardName(post.getBoard().getName())
                                  .viewCount(post.getViewCount())
                                  .isNotice(post.isNotice())
                                  .createdAt(formatCreatedAt)
                                  .userNickname(userNickname)
                                  .reactionCount(reactionCount)
                                  .boardId(post.getBoard().getBoardId())
                                  .build();
    }

    // 일반 게시글용(닉네임, 댓글 카운터 O)
    public static final PostListResponseDTO fromEntity(Post post, int reactionCount, String userNickname, int commentCount) {

    	String formatCreatedAt = DATE_TIME_FORMATTER.format(post.getCreatedAt());

        return PostListResponseDTO.builder()
                                  .postId(post.getPostId())
                                  .title(post.getTitle())
                                  .boardName(post.getBoard().getName())
                                  .viewCount(post.getViewCount())
                                  .isNotice(post.isNotice())
                                  .createdAt(formatCreatedAt)
                                  .reactionCount(reactionCount)
                                  .userNickname(userNickname)
                                  .commentCount(commentCount)
                                  .boardId(post.getBoard().getBoardId())
                                  .build();
    }

    // 키워드, 닉네임 사용자 검색 게시글용(닉네임, 댓글 카운터, 대표 이미지 O)
    public static PostListResponseDTO fromEntity(Post post, int reactionCount, String userNickname, int commentCount, String thumbnailUrl) {

    	String formatCreatedAt = DATE_TIME_FORMATTER.format(post.getCreatedAt());

        return PostListResponseDTO.builder()
                                  .postId(post.getPostId())
                                  .title(post.getTitle())
                                  .boardName(post.getBoard().getName())
                                  .viewCount(post.getViewCount())
                                  .isNotice(post.isNotice())
                                  .createdAt(formatCreatedAt)
                                  .reactionCount(reactionCount)
                                  .userNickname(userNickname)
                                  .commentCount(commentCount)
                                  .thumbnailImageUrl(thumbnailUrl)
                                  .boardId(post.getBoard().getBoardId())
                                  .build();
    }

}
