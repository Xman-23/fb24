package com.example.demo.dto.board;

import java.time.format.DateTimeFormatter;

import com.example.demo.domain.board.Board;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
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

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardResponseDTO {

	// 필드 Start
    private Long boardId;

    private String name;

    private String description;

    private boolean isActive;

    private int sortOrder;

    private String createdAt;
 
    private String updatedAt;

    private Long parentBoard; 
    // 필드 End

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static BoardResponseDTO convertToResponseDTO(Board board) {
 
    	String formatCreatedAt = DATE_TIME_FORMATTER.format(board.getCreatedAt());
    	String formatUpdatedAt = DATE_TIME_FORMATTER.format(board.getUpdatedAt());

        Long parentBoardId = null; // 부모가 없으면 null
        if (board.getParentBoard() != null) {
            parentBoardId = board.getParentBoard().getBoardId();
        }

    	BoardResponseDTO boardResponseDTO = new BoardResponseDTO(board.getBoardId(), 
    															 board.getName(), 
    															 board.getDescription(), 
    															 board.isActive(), 
    															 board.getSortOrder(), 
    															 formatCreatedAt, 
    															 formatUpdatedAt,
    															 parentBoardId);

    	return boardResponseDTO;
    }
}
