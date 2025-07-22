package com.example.demo.dto.board;

import java.util.List;
import java.util.stream.Collectors;

import com.example.demo.domain.board.Board;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/*
	자식게시판 Response DTO

	Response(응답)
	boardId		(게시판 ID)
	name		(게시판 제목)
	description	(게시판 설명)
	isActive	(숨김 기능)
	sortOrder	(나열 기능)
	childBoards	(자식 게시판들)
*/
@Setter
@Getter
public class BoardHierarchyResponsetDTO {

	private Long boardId;

	private String name;

	private String description;

	private boolean isActive;
	
	private int sortOrder;

	private List<BoardHierarchyResponsetDTO> childBoards; // 자식 게시판들

	public static BoardHierarchyResponsetDTO convertToHierarchy (Board board) {

		BoardHierarchyResponsetDTO boardHierarchyResponsetDTO = new BoardHierarchyResponsetDTO();
		boardHierarchyResponsetDTO.setBoardId(board.getBoardId());
		boardHierarchyResponsetDTO.setName(board.getName());
		boardHierarchyResponsetDTO.setDescription(board.getDescription());
		boardHierarchyResponsetDTO.setActive(board.isActive());
		boardHierarchyResponsetDTO.setSortOrder(board.getSortOrder());

		List<BoardHierarchyResponsetDTO> children = board.getChildBoards()
		                                                 .stream()
		                                                 .map(BoardHierarchyResponsetDTO::convertToHierarchy)
		                                                 .collect(Collectors.toList());
		boardHierarchyResponsetDTO.setChildBoards(children);

		return boardHierarchyResponsetDTO;
	}

}
