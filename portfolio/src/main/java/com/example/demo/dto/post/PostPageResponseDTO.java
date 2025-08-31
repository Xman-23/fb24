package com.example.demo.dto.post;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;

import org.springframework.data.domain.Page;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostPageResponseDTO {

	// 인기글 리스트
	private List<PostListResponseDTO> popularPosts;
	// 일반 게시글 리스트
	private List<PostListResponseDTO> normalPosts;

	// 수동 페이징 처리
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

	public static PostPageResponseDTO fromPage(List<PostListResponseDTO> popularPosts,
			                                   Page<PostListResponseDTO> normalPostPage) {
		return PostPageResponseDTO.builder()
								  .popularPosts(popularPosts != null ? popularPosts : Collections.emptyList())
				                  .normalPosts(normalPostPage.getContent())
				                  .pageNumber(normalPostPage.getNumber())
				                  .pageSize(normalPostPage.getSize())
				                  .totalElements(normalPostPage.getTotalElements())
				                  .totalPages(normalPostPage.getTotalPages())
				          	      // 이전 버튼 
				          	      //ex) 현재 2페이지(index=1)이면, 1페이지 존재(index=0) 'true'
				                  .hasPrevious(normalPostPage.getNumber() > 0)
				          	      // 앞 버튼 
				          	      //ex) 현재 20페이지(index=19)이면, 총페이지 20페이지(index = totalPages(20) -1) 'false'
				                  .hasNext(normalPostPage.getNumber() < normalPostPage.getTotalPages()-1)
				          	      // 맨 앞 버튼 
				          	      // ex) 현재 10페이지(index=9)이면 ,1페이지 존재(index=0) 'true', 현재 1페이지이면(index=0) 'false'
				                  .hasFirst(normalPostPage.getNumber() > 0)
				          	      // 맨 뒤 버튼
				          	      // ex) 현재 5페이지(index=4)이면, 총 페이지 20페이지(index = totalPages(20)-1) 'true', 현재 20페이지(index=19) 'false'
				                  .hasLast(normalPostPage.getNumber() < normalPostPage.getTotalPages()-1)
				          	      // 10 페이지 앞으로 버튼
				          	      // ex) 현재 4페이지(index=3)이면, 10페이지 앞으로 버튼 누를시,
				          	      // '3(currnetPage)-10'의해 '음수', 그러므로 페이지가 벗어나는걸 방지하기 위해 '첫페이지(index=0)' 설정
				                  .jumpBackwardPage(Math.max(normalPostPage.getNumber()-10, 0))
				                  // 10 페이지 뒤로 버튼
				          	      // ex) 현재 11페이지(index=10)이면, 10페이지 뒤로 버튼 눌르시,
				          	      // '10(currentPage)+10'의해 index = 20이므로, 총페이지 20페이지(index = 19) 범위를 벗어나는것을 방지하기 위한 '맨뒤 페이지(totalPages-1)'설정
				                  .jumpForwardPage(normalPostPage.getTotalPages() == 0 ? 0 : Math.min(normalPostPage.getNumber() +10 , normalPostPage.getTotalPages()-1))
				                  .build();
	}
}
