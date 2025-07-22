package com.example.demo.dto.post;

import java.time.LocalDateTime;

import com.example.demo.domain.post.Post;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    private int likeCount;
    private int dislikeCount;
    private boolean isNotice;
    private LocalDateTime createdAt;

    public static PostListResponseDTO fromEntity(Post post) {
        return PostListResponseDTO.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .boardName(post.getBoard().getName())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .dislikeCount(post.getDislikeCount())
                .isNotice(post.isNotice())
                .createdAt(post.getCreatedAt())
                .build();
    }

}
