package com.example.demo.dto.comment;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.example.demo.domain.comment.Comment;
import com.example.demo.domain.comment.commentenums.CommentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO: 댓글 응답 데이터 전송 객체
 *
 * 필드 설명:
 * commentId        - 댓글 ID
 * postId           - 댓글이 속한 게시글 ID
 * parentCommentId  - 대댓글인 경우 부모 댓글 ID (댓글이면 null)
 * authorId         - 작성자 ID
 * content          - 댓글 내용
 * createdAt        - 작성 시각
 * updatedAt        - 수정 시각
 * childComments    - 자식 댓글(대댓글) 리스트
 * likeCount        - 좋아요 수
 * dislikeCount		- 싫어요 수
 * isPinned         - 고정 여부
 * updatedAgo       - "몇 분 전 수정됨" 같은 상대 시간 표시
 */



@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponseDTO {

    private Long commentId;

    private Long postId;

    private Long parentCommentId; // null이면 부모댓글

    private Long authorId;

    private String content;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<CommentResponseDTO> childComments; // 대댓글 리스트

    private int likeCount;

    private int dislikeCount;

    private boolean isPinned;

    private String updatedAgo; // "몇 분 전 수정됨" 등

    private CommentStatus status;

    // Entity -> DTO 변환 메서드
    public static CommentResponseDTO fromEntity(Comment comment, int likeCount, int dislikeCount, boolean isPinned) {
        CommentResponseDTO dto = CommentResponseDTO.builder()
                                                   .commentId(comment.getCommentId())
                                                   .postId(comment.getPost().getPostId())
                                                   .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getCommentId() : null)
                                                   .authorId(comment.getAuthorId())
                                                   .content(comment.getContent())
                                                   .createdAt(comment.getCreatedAt())
                                                   .updatedAt(comment.getUpdatedAt())
                                                   .likeCount(likeCount)
                                                   .dislikeCount(dislikeCount)
                                                   .isPinned(isPinned)
                                                   .status(comment.getStatus())
                                                   .build();

        // 수정된 시간과 생성 시간 비교해서 updatedAgo 문자열 만들기
        if (!comment.getUpdatedAt().equals(comment.getCreatedAt())) {
        	// Duration.between(A,B)는 -> A-B가 아닌, B-A로 계산이 된다
            Duration duration = Duration.between(comment.getCreatedAt(), comment.getUpdatedAt()).abs();
            long minutes = duration.toMinutes();
            if(minutes < 1){ 
            	dto.setUpdatedAgo("방금 전 수정됨");
        	}else if (minutes < 60) {
                dto.setUpdatedAgo(minutes + "분 전 수정됨");
            } else if ( minutes < 1440){
                long hours = duration.toHours();
                dto.setUpdatedAgo(hours + "시간 전 수정됨");
            } else {
            	long days = duration.toDays();
            	dto.setUpdatedAgo(days + "일 전 수정됨");
            }
        } else {
            dto.setUpdatedAgo(null);
        }

        // 자식 댓글 재귀 변환
        if (comment.getChildComments() != null && !comment.getChildComments().isEmpty()) {
            List<CommentResponseDTO> childDtos = comment.getChildComments().stream()
                                                        .map(child -> fromEntity(child, 0, 0, false)) // 좋아요, 고정 여부는 서비스에서 따로 처리 필요
                                                        .collect(Collectors.toList());
            dto.setChildComments(childDtos);
        }

        return dto;
    }
}