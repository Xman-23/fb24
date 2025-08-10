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
public class PostParentBoardPostPageResponseDTO {

	// 1. 상단 고정 공지글(공지게시판에서 pin된글)
	private List<PostListResponseDTO> topNotices;

	// 2. 자식 게시판 인기글(좋아요 + 신선도 기준)
	private List<PostListResponseDTO> popularPosts;

	// 페이징 정보
	// 현재 페이지 
	private int pageNumber;
	// 한 페이지당 보여줄 size
	private int pageSize;
	// 총 데이터 갯수
	private long totalElements;
	// 총 페이지 갯수
	private int totalPages;

	// 페이징 버튼 정보
    private boolean hasPrevious;
    private boolean hasNext;
    private boolean hasFirst;
    private boolean hasLast;

    private int jumpBackwardPage;
    private int jumpForwardPage;

    public static PostParentBoardPostPageResponseDTO fromDTO (List<PostListResponseDTO> topNotices,
    		                                              Page<PostListResponseDTO> popularPostPage) {
    	return PostParentBoardPostPageResponseDTO.builder()
    			                             .topNotices(topNotices == null ? Collections.emptyList() : topNotices)
    			                              // 한 페이지당 받는 데이터 갯수
    			                             .popularPosts(popularPostPage.getContent())
    			                              // 현재 페이지 넘버 ex) 5페이지(index = 4)
    			                             .pageNumber(popularPostPage.getNumber())
    			                              // 한 페이지당 데이터를 보여줄 사이즈
    			                             .pageSize(popularPostPage.getSize())
    			                              // 총 데이터 갯수
    			                             .totalElements(popularPostPage.getTotalElements())
    			                              // 총 데이터 갯수/ 한페이지당 보여줄 Size = 총 페이지 갯수
    			                             .totalPages(popularPostPage.getTotalPages())
    						          	      // 이전 버튼 
    						          	      //ex) 현재 2페이지(index=1)이면, 1페이지 존재(index=0) 'true'
    						                  .hasPrevious(popularPostPage.getNumber() > 0)
    						          	      // 앞 버튼 
    						          	      //ex) 현재 20페이지(index=19)이면, 총페이지 20페이지(index = totalPages(20) -1) 'false'
    						                  .hasNext(popularPostPage.getNumber() < popularPostPage.getTotalPages()-1)
    						          	      // 맨 앞 버튼 
    						          	      // ex) 현재 10페이지(index=9)이면 ,1페이지 존재(index=0) 'true', 현재 1페이지이면(index=0) 'false'
    						                  .hasFirst(popularPostPage.getNumber() > 0)
    						          	      // 맨 뒤 버튼
    						          	      // ex) 현재 5페이지(index=4)이면, 총 페이지 20페에지(index = totalPages(20)-1) 'true', 현재 20페이지(index=19) 'false'
    						                  .hasLast(popularPostPage.getNumber() < popularPostPage.getTotalPages()-1)
    						          	      // 10 페이지 앞으로 버튼
    						          	      // ex) 현재 4페이지(index=3)이면, 10페이지 앞으로 버튼 누를시,
    						          	      // '3(currnetPage)-10'의해 '음수', 그러므로 페이지가 벗어나는걸 방지하기 위해 '첫페이지(index=0)' 설정
    						                  .jumpBackwardPage(Math.max(popularPostPage.getNumber()-10, 0))
    						                  // 10 페이지 뒤로 버튼
    						          	      // ex) 현재 11페이지(index=10)이면, 10페이지 뒤로 버튼 눌르시,
    						          	      // '10(currentPage)+10'의해 index = 20이므로, 총페이지 20페이지(index = 19) 범위를 벗어나는것을 방지하기 위한 '맨뒤 페이지(totalPages-1)'설정
    						                  .jumpForwardPage(popularPostPage.getTotalPages() == 0 ? 0 : Math.min(popularPostPage.getTotalPages()-1, popularPostPage.getNumber()+10))
    						                  .build();
    }
}
