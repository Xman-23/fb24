package com.example.demo.dto;

import java.time.LocalDateTime;

import com.example.demo.domain.post.Post;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MainPopularPostDTO {

    private Long postId;            // 게시글 ID
    private String title;           // 게시글 제목
    private String authorNickname;  // 작성자 닉네임
    private int likeCount;          // 좋아요 수
    private int commentCount;       // 댓글 수
    private String thumbnailUrl;    // 대표 이미지 URL (없으면 null 가능)
    private LocalDateTime createdAt;// 게시글 작성일

    // fromEntity 변환 메서드 (필요한 리포지토리에서 데이터 가져와 매핑)
    public static MainPopularPostDTO fromEntity(Post post, 
                                                int likeCount, 
                                                int dislikeCount,
                                                int commentCount, 
                                                String authorNickname,
                                                String thumbnailUrl) {
        return MainPopularPostDTO.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .authorNickname(authorNickname)
                .likeCount(likeCount)
                .commentCount(commentCount)
                .thumbnailUrl(thumbnailUrl)
                .createdAt(post.getCreatedAt())
                .build();
    }
}
