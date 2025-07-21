package com.example.demo.service.board;

import java.util.List;

import com.example.demo.dto.board.BoardCreateRequestDTO;
import com.example.demo.dto.board.BoardResponseDTO;
import com.example.demo.dto.board.BoardUpdateRequestDTO;

public interface BoardService {

	// 게시판 생성
	BoardResponseDTO createBoard(BoardCreateRequestDTO boardCreateRequestDTO);

	// 게시판 수정
	BoardResponseDTO updateBoard(Long boardId, BoardUpdateRequestDTO boardUpdateRequestDTO);

	// 게시판 단건 조회
	BoardResponseDTO getBoard(Long boardId);

	// 전체 게시판 목록 조회
	List<BoardResponseDTO> getAllBoards();

	// 게시판 삭제
	void deleteBoard(Long boardId);

}
