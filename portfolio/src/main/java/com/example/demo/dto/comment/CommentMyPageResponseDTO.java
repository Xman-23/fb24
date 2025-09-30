package com.example.demo.dto.comment;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentMyPageResponseDTO {
    private List<CommentListResponseDTO> comments;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean hasPrevious;
    private boolean hasNext;
    private boolean hasFirst;
    private boolean hasLast;
    private int jumpBackwardPage;
    private int jumpForwardPage;

    public static CommentMyPageResponseDTO fromPage(Page<CommentListResponseDTO> page) {
        return CommentMyPageResponseDTO.builder()
                                       .comments(page.getContent())
                                       .pageNumber(page.getNumber())
                                       .pageSize(page.getSize())
                                       .totalElements(page.getTotalElements())
                                       .totalPages(page.getTotalPages())
                                       .hasPrevious(page.getNumber() > 0)
                                       .hasNext(page.getNumber() < page.getTotalPages() - 1)
                                       .hasFirst(page.getNumber() > 0)
                                       .hasLast(page.getNumber() < page.getTotalPages() - 1)
                                       .jumpBackwardPage(Math.max(page.getNumber() - 10, 0))
                                       .jumpForwardPage(page.getTotalPages() == 0 ? 0 : Math.min(page.getNumber() + 10, page.getTotalPages() - 1))
                                       .build();
    }
}
