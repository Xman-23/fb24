package com.example.demo.dto.post;

import java.time.LocalDateTime;

import com.example.demo.domain.post.Post;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostListResponseDTO {

    private Long postId;

    private String title;

    private String boardName;

    private int viewCount;

    private boolean isNotice;

    private LocalDateTime createdAt;

    private int reactionCount;

    private String userNickname;
 

    public static PostListResponseDTO fromEntity(Post post, int reactionCount) {
        return PostListResponseDTO.builder()
                                  .postId(post.getPostId())
                                  .title(post.getTitle())
                                  .boardName(post.getBoard().getName())
                                  .viewCount(post.getViewCount())
                                  .isNotice(post.isNotice())
                                  .createdAt(post.getCreatedAt())
                                  .reactionCount(reactionCount)
                                  .build();
    }

    public static PostListResponseDTO fromEntity(Post post, int reactionCount, String userNickname) {
        return PostListResponseDTO.builder()
                                  .postId(post.getPostId())
                                  .title(post.getTitle())
                                  .boardName(post.getBoard().getName())
                                  .viewCount(post.getViewCount())
                                  .isNotice(post.isNotice())
                                  .createdAt(post.getCreatedAt())
                                  .reactionCount(reactionCount)
                                  .userNickname(userNickname)
                                  .build();
    }

}
