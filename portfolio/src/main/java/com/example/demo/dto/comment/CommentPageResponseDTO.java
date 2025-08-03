package com.example.demo.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentPageResponseDTO {

    private List<CommentResponseDTO> comments;

    private int pageNumber;

    private int pageSize;

    private long totalElements;

    private int totalPages;

}
