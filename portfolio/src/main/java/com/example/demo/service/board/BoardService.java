package com.example.demo.service.board;

import java.util.List;


import com.example.demo.dto.board.BoardCreateRequestDTO;
import com.example.demo.dto.board.BoardHierarchyResponseDTO;
import com.example.demo.dto.board.BoardResponseDTO;
import com.example.demo.dto.board.BoardUpdateRequestDTO;

public interface BoardService {

	// 게시판 생성
	BoardResponseDTO createBoard(BoardCreateRequestDTO boardCreateRequestDTO);

	// 게시판 수정
	BoardResponseDTO updateBoard(Long boardId, BoardUpdateRequestDTO boardUpdateRequestDTO);

	// 게시판 삭제
	void deleteBoard(Long boardId);
	
	// 게시판 단건 조회
	BoardResponseDTO getBoard(Long boardId);

	// 특정 게시판 계층 조회
	BoardHierarchyResponseDTO getBoardHierarchyByParent(Long boardId);

	// 부모 게시판 목록 조회
	List<BoardResponseDTO> getParentBoards();

	// 전체 게시판 (부모 게시판 + 자식 게시판) 조회
	List<BoardHierarchyResponseDTO> getBoardFullHierarchy();

}
