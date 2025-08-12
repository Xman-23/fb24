package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;

import com.example.demo.domain.post.Post;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MainPopularPostPageResponseDTO {

    private List<MainPopularPostDTO> posts;  // 게시글 리스트

    private int pageNumber;

    private int pageSize;

    private long totalElements;

    private int totalPages;

    // 버튼 설정 Start
    private boolean hasPrevious;
    private boolean hasNext;
    private boolean hasFirst;
    private boolean hasLast;

    private int jumpBackwardPage;
    private int jumpForwardPage;
    // 버튼 설정 End

    public static MainPopularPostPageResponseDTO fromPage(Page<MainPopularPostDTO> postPage) {

    	List<MainPopularPostDTO> dtos = postPage.getContent();

    	int currentPage = postPage.getNumber();
    	int totalPage = postPage.getTotalPages();

        return MainPopularPostPageResponseDTO.builder()
                							 .posts(dtos)
                                             .pageNumber(currentPage)
                                             .totalPages(totalPage)
                                             .totalElements(postPage.getTotalElements())
                                             .pageSize(postPage.getSize())
           				          	         // 이전 버튼 
           				          	         //ex) 현재 2페이지(index=1)이면, 1페이지 존재(index=0) 'true'
           				                     .hasPrevious(currentPage > 0)
           				          	         // 앞 버튼 
           				          	         //ex) 현재 20페이지(index=19)이면, 총페이지 20페이지(index = totalPages(20) -1) 'false'
           				                     .hasNext(currentPage < totalPage-1)
           				          	         // 맨 앞 버튼 
           				          	         // ex) 현재 10페이지(index=9)이면 ,1페이지 존재(index=0) 'true', 현재 1페이지이면(index=0) 'false'
           				                     .hasFirst(currentPage > 0)
           				          	         // 맨 뒤 버튼
           				          	         // ex) 현재 5페이지(index=4)이면, 총 페이지 20페에지(index = totalPages(20)-1) 'true', 현재 20페이지(index=19) 'false'
           				                     .hasLast(currentPage < totalPage-1)
           				          	         // 10 페이지 앞으로 버튼
           				          	         // ex) 현재 4페이지(index=3)이면, 10페이지 앞으로 버튼 누를시,
           				          	         // '3(currnetPage)-10'의해 '음수', 그러므로 페이지가 벗어나는걸 방지하기 위해 '첫페이지(index=0)' 설정
           				                     .jumpBackwardPage(Math.max(currentPage-10, 0))
           				                     // 10 페이지 뒤로 버튼
           				          	         // ex) 현재 11페이지(index=10)이면, 10페이지 뒤로 버튼 눌르시,
           				          	         // '10(currentPage)+10'의해 index = 20이므로, 총페이지 20페이지(index = 19) 범위를 벗어나는것을 방지하기 위한 '맨뒤 페이지(totalPages-1)'설정
           				                     .jumpForwardPage(totalPage == 0 ? 0 : Math.min(currentPage +10 , totalPage-1))
           				                     .build();
    }
}