package com.example.demo.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentGoPageResponseDTO {
    private Long commentId;       // 조회한 댓글 ID
    private int pageNumber;       // 해당 댓글이 있는 페이지 번호
    private int totalPages;       // 전체 페이지 수
    private int positionInPage;   // 해당 페이지에서 댓글 위치 (순번)

}
