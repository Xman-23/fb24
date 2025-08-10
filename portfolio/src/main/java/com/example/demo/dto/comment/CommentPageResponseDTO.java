package com.example.demo.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentPageResponseDTO {

    private List<CommentResponseDTO> comments;

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

}
