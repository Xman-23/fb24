package com.example.demo.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.*;

import org.springframework.data.domain.Pageable;

import com.example.demo.dto.post.PostListResponseDTO;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentPageResponseDTO {

	// 인기글 리스트
	private List<CommentResponseDTO> popularComments;
	
    private List<CommentResponseDTO> comments;

    private int pageNumber;

    private int pageSize;

    private long totalElements;

    private int totalPages;

    private long activeTotalElements;

    // 버튼 설정 Start
    private boolean hasPrevious;
    private boolean hasNext;
    private boolean hasFirst;
    private boolean hasLast;

    private int jumpBackwardPage;
    private int jumpForwardPage;
    // 버튼 설정 End

    // 전체 댓글 리스트와 Pageable 받아서 DTO 생성
    public static CommentPageResponseDTO fromEntityToPage(List<CommentResponseDTO> topPinned,
    													  List<CommentResponseDTO> sortedRoot, 
    		                                              Pageable pageable,
    		                                              long activeTotalElements ) {
    	
        // NullPointException 방지
        if (topPinned == null) {
        	topPinned = Collections.emptyList();
        }
        if (sortedRoot == null) {
        	sortedRoot = Collections.emptyList();
        }

    	// 페이징 수동 처리 (부모 댓글 기준)
    	int totalParentCommentElements  = sortedRoot.size();
    	// 한 페이지당 보여줄 사이즈
        int pageSize = pageable.getPageSize();
        // 현재 페이지 (인덱스 기준)
        int currentPage = pageable.getPageNumber();
        // 데이터가 총 27개 있을시, 3페이지까지 존재해야하므로 무조건 올림
        int totalPages = (int) Math.ceil((double) totalParentCommentElements / pageSize);

        // 페이지당 10개 보여주기('index'기준) 시작 인덱스 계산 
        // ex) 1페이지(0~9)10개, 2페이지(10~19)20개
        int start = (int) pageable.getOffset();
        // 끝 인덱스 계산: 시작 인덱스 + 페이지 크기, 단 전체 댓글 개수를 넘지 않도록 제한
        // ex) 전체 댓글이 27개면 20(start) + 10(pageSize) = 30이지만 27(totalParentCommentElements)로 제한
        int end = Math.min(start + pageSize, totalParentCommentElements);

        List<CommentResponseDTO> pagedComments = null;
	    
        // start 가 총 데이터 갯수보다 많거나 같다면은 그건, 데이터가 없다는것이므로 빈페이지 반환
        // ex) totalParentCommentElements 총 부모 댓글이 27개 인데, 
        //     'start'가 27보다 같거나 크다면은 부모 댓글이 28개 라는 뜻이므로 빈 리스트를 반환해야한다
	    if (start >= totalParentCommentElements) {
	    	pagedComments = Collections.emptyList();
	    } else {
	    	// 그렇지 않다면은 10개씩 데이터 잘라서 'List'에 넣어주기
	    	pagedComments = sortedRoot.subList(start, end);
	    }

	    // 현재 페이지가 1페이지(0인덱스)보다 커야지 '이전 버튼' 활성화
	    // ex) 현재 2페이지(index=1)이면, 1페이지 존재(index=0) 'true'
        boolean hasPrevious = currentPage > 0;
        // 현재 페이지가 totalPages-1('index'기준) 보다 작아야지 다음 버튼 활성화
        // ex) 현재 20페이지(index=19)이면, 총페이지 20페이지(index = totalPages(20) -1) 'false'
        boolean hasNext = currentPage < totalPages - 1;
        // 현재 페이지가 1페이지(0인덱스)보다 커야지 '맨 앞 버튼' 활성화 (hasPrevious 동일)
        // ex) 현재 10페이지(index=9)이면 ,1페이지 존재(index=0) 'true', 현재 1페이지이면(index=0) 'false'
        boolean hasFirst = hasPrevious;
        // 현재 페이지가 마지막 페이지보다 작아야지 맨뒤 버튼 활성화
        // ex) 현재 5페이지(index=4)이면, 총 페이지 20페이지(index = totalPages(20)-1) 'true', 현재 20페이지(index=19) 'false'
        boolean hasLast = hasNext;

	    // 10 페이지 앞으로 버튼
	    // ex) 현재 4페이지(index=3)이면, 10페이지 앞으로 버튼 누를시,
	    // '3(currnetPage)-10'의해 '음수', 그러므로 페이지가 벗어나는걸 방지하기 위해 '첫페이지(index=0)' 설정
        int jumpBackwardPage = Math.max(currentPage - 10, 0);

        // 10 페이지 뒤로 버튼
	    // ex) 현재 11페이지(index=10)이면, 10페이지 뒤로 버튼 눌르시,
	    // '10(currentPage)+10'의해 index = 20이므로, 총페이지 20페이지(index = 19) 범위를 벗어나는것을 방지하기 위한 '맨뒤 페이지(totalPages-1)'설정
        int jumpForwardPage = totalPages == 0 ? 0 : Math.min(currentPage + 10, totalPages - 1);

        return CommentPageResponseDTO.builder()
        							 .popularComments(topPinned)
                                     .comments(pagedComments)
                                     .pageNumber(currentPage)
                                     .pageSize(pageSize)
                                     .totalElements(totalParentCommentElements)
                                     .totalPages(totalPages)
                                     .hasPrevious(hasPrevious)
                                     .hasNext(hasNext)
                                     .hasFirst(hasFirst)
                                     .hasLast(hasLast)
                                     .jumpBackwardPage(jumpBackwardPage)
                                     .jumpForwardPage(jumpForwardPage)
                                     .activeTotalElements(activeTotalElements)
                                     .build();
    }
}
