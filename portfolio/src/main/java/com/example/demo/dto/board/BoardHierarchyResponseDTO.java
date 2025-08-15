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
public class BoardHierarchyResponseDTO {

	private Long boardId;

	private String name;

	private String description;

	private boolean isActive;
	
	private int sortOrder;

	private List<BoardHierarchyResponseDTO> childBoards; // 자식 게시판들

	public static BoardHierarchyResponseDTO convertToHierarchy (Board board) {

		BoardHierarchyResponseDTO boardHierarchyResponseDTO = new BoardHierarchyResponseDTO();
		boardHierarchyResponseDTO.setBoardId(board.getBoardId());
		boardHierarchyResponseDTO.setName(board.getName());
		boardHierarchyResponseDTO.setDescription(board.getDescription());
		boardHierarchyResponseDTO.setActive(board.isActive());
		boardHierarchyResponseDTO.setSortOrder(board.getSortOrder());

		List<BoardHierarchyResponseDTO> children = board.getChildBoards()
		                                                 .stream()
		                                                 .map(BoardHierarchyResponseDTO::convertToHierarchy)
		                                                 .collect(Collectors.toList());
		boardHierarchyResponseDTO.setChildBoards(children);

		return boardHierarchyResponseDTO;
	}

}
