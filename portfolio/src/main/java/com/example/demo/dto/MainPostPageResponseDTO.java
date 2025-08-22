package com.example.demo.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.example.demo.dto.post.PostListResponseDTO;

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
public class MainPostPageResponseDTO {

    private List<PostListResponseDTO> posts;  // 인기글/검색결과 통합

    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;

    // 페이징 버튼
    private boolean hasPrevious;
    private boolean hasNext;
    private boolean hasFirst;
    private boolean hasLast;

    private int jumpBackwardPage;
    private int jumpForwardPage;

    public static MainPostPageResponseDTO fromPage(Page<PostListResponseDTO> postPage) {
        int currentPage = postPage.getNumber();
        int totalPage = postPage.getTotalPages();

        return MainPostPageResponseDTO.builder()
                                         .posts(postPage.getContent())
                                         .pageNumber(currentPage)
                                         .pageSize(postPage.getSize())
                                         .totalElements(postPage.getTotalElements())
                                         .totalPages(totalPage)
                                         .hasPrevious(currentPage > 0)
                                         .hasNext(currentPage < totalPage - 1)
                                         .hasFirst(currentPage > 0)
                                         .hasLast(currentPage < totalPage - 1)
                                         .jumpBackwardPage(Math.max(currentPage - 10, 0))
                                         .jumpForwardPage(totalPage == 0 ? 0 : Math.min(currentPage + 10, totalPage - 1))
                                         .build();
    }
}