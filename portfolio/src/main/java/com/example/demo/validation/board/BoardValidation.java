package com.example.demo.validation.board;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.demo.dto.board.BoardResponseDTO;

public class BoardValidation {

	// 게시판 ID(boardId) 유효성 검사
	public static boolean isValidBoardId (Long boardId) {
		if( boardId == null || boardId <= 0 ) {
			return false;
		}
		return true;
	}

	// 전체 게시판 유효성 검사
	public static boolean isValidAllBoard(List<BoardResponseDTO> boards) {
		if(boards == null) {
			return false;
		}
		return true;
	}

}
