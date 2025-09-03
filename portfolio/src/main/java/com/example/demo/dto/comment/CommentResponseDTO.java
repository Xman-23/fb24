package com.example.demo.dto.comment;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private String authorNickname;

    private String content;

    private String createdAt;

    private List<CommentResponseDTO> childComments; // 대댓글 리스트

    private int likeCount;

    private int dislikeCount;

    private boolean isPinned;

    private String updatedAgo; // "몇 분 전 수정됨" 등

    private CommentStatus status;

    private static final Logger logger = LoggerFactory.getLogger(CommentResponseDTO.class);
    
    
    // 복사 생성자
    public CommentResponseDTO(CommentResponseDTO other) {
    	logger.info("CommentResponseDTO 복사 생성자() Start");
        this.commentId = other.commentId;
        this.postId = other.postId;
        this.parentCommentId = other.parentCommentId;
        this.authorId = other.authorId;
        this.authorNickname = other.authorNickname;
        this.content = other.content;
        this.createdAt = other.createdAt;
        this.likeCount = other.likeCount;
        this.dislikeCount = other.dislikeCount;
        this.isPinned = other.isPinned;
        this.status = other.status;
        this.childComments = other.childComments != null 
                								 ? new ArrayList<>(other.childComments) 
                								 : new ArrayList<>();

        // 상태에 맞는 필드 보정 처리
        handleCommentStatus(other);
        logger.info("CommentResponseDTO 복사 생성자() End");
    }

 // 상태에 맞는 필드 설정
    private void handleCommentStatus(CommentResponseDTO other) {
        switch (other.status) {
            case ACTIVE:
                this.updatedAgo = calculateUpdatedAgo(other.createdAt, other.updatedAgo);
                break;

            case DELETED:
                this.content = "삭제된 댓글입니다.";
                this.createdAt = "";
                this.updatedAgo = "";
                break;

            case HIDDEN:
                this.content = "신고받은 댓글입니다.";
                this.createdAt = "";
                this.updatedAgo = "";
                break;
        }
    }
    
    
    // ==============================
    // updatedAgo 계산
    // ==============================
    private String calculateUpdatedAgo(String createdAtStr, String updatedAtStr) {
        if (createdAtStr == null || updatedAtStr == null) return null;

        // createdAt / updatedAt이 String → LocalDateTime으로 변환된다고 가정
        LocalDateTime createdAt = LocalDateTime.parse(createdAtStr);
        LocalDateTime updatedAt = LocalDateTime.parse(updatedAtStr);

        if (updatedAt.equals(createdAt)) {
            return null; // 수정되지 않음
        }

        Duration duration = Duration.between(createdAt, updatedAt).abs();
        long minutes = duration.toMinutes();

        if (minutes < 1) {
            return "방금 전 수정됨";
        } else if (minutes < 60) {
            return minutes + "분 전 수정됨";
        } else if (minutes < 1440) {
            return duration.toHours() + "시간 전 수정됨";
        } else {
            long days = duration.toDays();
            if (days < 10) {
                return days + "일 전 수정됨";
            } else if (days < 365) {
                return (days / 7) + "주 전 수정됨";
            } else {
                return (days / 365) + "년 전 수정됨";
            }
        }
    }

    // 시간 변경
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Entity -> DTO 변환 메서드
    public static CommentResponseDTO fromEntity(Comment comment, 
    		                                    int likeCount, 
    		                                    int dislikeCount, 
    		                                    boolean isPinned, 
    		                                    String authorNickname) {
    	logger.info("CommentResponseDTO fromEntity() Start");

    	// 여기에 시간 포맷 추가
        CommentResponseDTO dto = CommentResponseDTO.builder()
                                                   .commentId(comment.getCommentId())
                                                   .postId(comment.getPost().getPostId())
                                                   .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getCommentId() : null)
                                                   .authorId(comment.getMember().getId())
                                                   .createdAt(comment.getCreatedAt().format(formatter))
                                                   .likeCount(likeCount)
                                                   .dislikeCount(dislikeCount)
                                                   .isPinned(isPinned)
                                                   .status(comment.getStatus())
                                                   .authorNickname(authorNickname)
                                                   .content(comment.getContent())
                                                   .build();
        logger.info("CommentResponseDTO fromEntity() End");
        return dto;
    }

}