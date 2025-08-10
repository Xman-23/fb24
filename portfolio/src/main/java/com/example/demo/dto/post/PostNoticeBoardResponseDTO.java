package com.example.demo.dto.post;

import java.util.Collections;
import java.util.List;

import org.springframework.data.domain.Page;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostNoticeBoardResponseDTO {

    // 1. 상단 고정 공지글 (pin == true)
    private List<PostListResponseDTO> topPinnedNotices;

    // 2. 일반 공지글 (pin == false)
    private List<PostListResponseDTO> notices;

    // 3. 페이징 정보
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;

    // 4. 페이지 버튼 정보
    private boolean hasPrevious;
    private boolean hasNext;
    private boolean hasFirst;
    private boolean hasLast;

    private int jumpBackwardPage;
    private int jumpForwardPage;

    // static factory method
    public static PostNoticeBoardResponseDTO from(List<PostListResponseDTO> topPinnedNotices,
    										  Page<PostListResponseDTO> noticePage) {
        return PostNoticeBoardResponseDTO.builder()
                .topPinnedNotices(topPinnedNotices == null ? Collections.emptyList() : topPinnedNotices)
                .notices(noticePage.getContent())
                .pageNumber(noticePage.getNumber())
                .pageSize(noticePage.getSize())
                .totalElements(noticePage.getTotalElements())
                .totalPages(noticePage.getTotalPages())
                .hasPrevious(noticePage.getNumber() > 0)
                .hasNext(noticePage.getNumber() < noticePage.getTotalPages() - 1)
                .hasFirst(noticePage.getNumber() > 0)
                .hasLast(noticePage.getNumber() < noticePage.getTotalPages() - 1)
                .jumpBackwardPage(Math.max(noticePage.getNumber() - 10, 0))
                .jumpForwardPage(noticePage.getTotalPages() == 0 ? 0 : Math.min(noticePage.getTotalPages() - 1, noticePage.getNumber() + 10))
                .build();
    }
}