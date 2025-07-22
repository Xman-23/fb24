package com.example.demo.dto.board;

import java.time.LocalDateTime;

import com.example.demo.domain.board.Board;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
	게시판 응답 DTO

	Response	(응답)
	boardId		(게시판ID)
	name		(게시판 제목)
	description	(게시판 설명)
	isActive	(게시판 숨김기능)
	sortOrder 	(게시판 순서나열기능)
	createdAt	(등록일자)
	updatedAt	(수정일자)
*/

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoardResponseDTO {

    private Long boardId;
    private String name;
    private String description;
    private boolean isActive;
    private int sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BoardResponseDTO convertToResponseDTO(Board board) {
 
    	BoardResponseDTO boardResponseDTO = new BoardResponseDTO(board.getBoardId(), 
    															 board.getName(), 
    															 board.getDescription(), 
    															 board.isActive(), 
    															 board.getSortOrder(), 
    															 board.getCreatedAt(), 
    															 board.getUpdatedAt());

    	return boardResponseDTO;
    }

}
