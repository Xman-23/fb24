package com.example.demo.controller.board;

import java.util.List;

import java.util.NoSuchElementException;

import org.apache.catalina.connector.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.board.BoardCreateRequestDTO;
import com.example.demo.dto.board.BoardHierarchyResponsetDTO;
import com.example.demo.dto.board.BoardResponseDTO;
import com.example.demo.dto.board.BoardUpdateRequestDTO;
import com.example.demo.service.board.BoardService;
import com.example.demo.validation.board.BoardValidation;
import com.example.demo.validation.string.SafeTrim;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController // 'Json' 요청,응답을 위한 'RestController'
@RequestMapping("/boards") // API이면서, API엔트포인트 (게시판 전체 조회, 게시판 생성)
@RequiredArgsConstructor // 'final', '@NonNull' 선언된 필드 생성자 생성
public class BoardController {

	private final BoardService boardService;
	private static final Logger logger = LoggerFactory.getLogger(BoardController.class);

	//*************************************************** API START ***************************************************//

	// 게시판 생성 API엔드포인트
	// '게시판 생성'은 관리자(Role_Admin)만 가능.
	@PostMapping("/admin/create-board")
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public ResponseEntity<?> createBoard(@RequestBody @Valid BoardCreateRequestDTO boardCreateRequestDTO,
															 BindingResult bindingResult) {
		/*
		 * 유효성 검사 조건 요약 *
		 상황 | @Valid | @NotBlank | BindingResult | 결과 설명
		------------------------------------------------------
		 1.   |   X   |     X     |       X       | 유효성 검사 안 함 (단, JSON '{}(body)'자체가 없으면 400 터짐)
		 2.   |   X   |     O     |       X       | 유효성 검사 안 함 (@NotBlank는 작동하지 않음 → 무의미)
		 3.   |   O   |     O     |       X       | 유효성 검사 작동 → 조건 위반 시 Spring이 자동으로 400 Bad Request 반환
		 4.   |   O   |     O     |       O       | 유효성 검사 작동 + 실패 시 내가 직접 에러 메시지 처리 가능
		*/

		logger.info("BoardController createBoard() Start");

		if(bindingResult.hasErrors()) {
			logger.error("BoardController createBoard() Error : 'BoardCreateRequestDTO'가 유효하지 않습니다.");
			return ResponseEntity.badRequest().body("입력값이 유효하지 않습니다.");
		}

		// DTO(Trim)
		String trimBoardName = SafeTrim.safeTrim(boardCreateRequestDTO.getName());
		String trimBoardDescription = SafeTrim.safeTrim(boardCreateRequestDTO.getDescription());
		Long   parentId = boardCreateRequestDTO.getParentBoardId();

		// After Trim
		boardCreateRequestDTO.setName(trimBoardName);
		boardCreateRequestDTO.setDescription(trimBoardDescription);

		if(parentId == null) {
			logger.info("'"+ parentId + "'가 'NUll'이므로, 부모 게시판 생성");
		}else {
			logger.info("'"+ parentId + "'가 '" + parentId + "이므로, 자식 게시판 생성");
		}

		boardCreateRequestDTO.setParentBoardId(parentId);

		// 'BoardServiceImpl.createBoard()'구현체 호출
		logger.info("BoardController createBoard() ==========> boardService.createBoard()");
		BoardResponseDTO response = null;

		try {
			response = boardService.createBoard(boardCreateRequestDTO);
		} catch (IllegalArgumentException e) {
			logger.error("BoardController createBoard() IllegalArgumentException Error : " + e.getMessage());
			// 중복(409)
			return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 존재하는 게시판 입니다.");
		} catch (NoSuchElementException e) {
			logger.error("BoardController createBoard() NoSuchElementException Error : " + e.getMessage());
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		}
		logger.info("BoardController createBoard() <========== boardService.createBoard()");

		logger.info("BoardController createBoard() Success End");
		return ResponseEntity.ok(response);
	}

	// 게시판 수정 API 엔드포인트
	@PatchMapping("/admin/{boardId}")
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public ResponseEntity<?> updateBoard (@PathVariable(name = "boardId") Long boardId,
										  @RequestBody BoardUpdateRequestDTO boardUpdateRequestDTO) {

		logger.info("BoardController updateBoard() Start");

		// 게시판 ID
		Long RequestBoardId = boardId;

		// 게시판 ID 유효성 검사
		if(!BoardValidation.isValidBoardId(RequestBoardId)) {
			logger.error("BoardController updateBoard() 'boardId' Invaild Input");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("입력값이 유효하지 않습니다.");
		}

		logger.info("BoardController updateBoard() ==========> boardService.updateBoard()");
		BoardResponseDTO response = null;

		try {
			response = boardService.updateBoard(boardId, boardUpdateRequestDTO);
		} catch (IllegalArgumentException e) {
			logger.error("BoardController updateBoard() IllegalArgumentException Catch   :" + e.getMessage());
			return ResponseEntity.status(HttpStatus.CONFLICT).body("게시판 이름이 중복 됩니다.");
		} catch (NoSuchElementException e) {
			logger.error("BoardController updateBoard() NoSuchElementException Catch   : " + e.getMessage());
			// '게시판'이 '존재' 하지 않아 찾을 수 없으므로, NotFound(404) 반환
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("수정할 게시판이 존재 하지 않습니다.");
		}
		logger.info("BoardController updateBoard() <========== boardService.updateBoard()");

		logger.info("BoardController updateBoard() Success End");
		return ResponseEntity.ok(response);
	}

    // 게시판 삭제
    @DeleteMapping("/admin/{boardId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> deleteBoard(@PathVariable(name = "boardId") Long boardId) {

    	logger.info("BoardController deleteBoard() Start");
		// 게시판 ID
		Long RequestBoardId = boardId;

		// 게시판 ID 유효성 검사
		if(!BoardValidation.isValidBoardId(RequestBoardId)) {
			logger.error("BoardController deleteBoard() boardId 입력값이 유효하지 않습니다.");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("입력값이 유효하지 않습니다.");
		}

		logger.info("BoardController deleteBoard() ==========> boardService.deleteBoard()");
		try {
			boardService.deleteBoard(RequestBoardId);
		} catch (NoSuchElementException e) {
			logger.error("BoardController deleteBoard() NoSuchElementException Catch   : " + e.getMessage());
			// '게시판'이 '존재' 하지 않아 찾을 수 없으므로, NotFound(404) 반환
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("삭제할 게시판이 존재 하지 않습니다.");
		}

		logger.info("BoardController deleteBoard() <========== boardService.deleteBoard()");

		logger.info("BoardController deleteBoard() Success End");
		return ResponseEntity.ok("게시판을 성공적으로 삭제했습니다.");
    }

	// 게시판 단건 조회
    @GetMapping("/{boardId}")
    public ResponseEntity<?> getBoard(@PathVariable(name = "boardId" ) Long boardId) {

    	logger.info("BoardController getBoard() Start");

		// 게시판 ID
		Long RequestBoardId = boardId;

		// 게시판 ID 유효성 검사
		if(!BoardValidation.isValidBoardId(RequestBoardId)) {
			logger.error("BoardController getBoard() 'boardId' Invaild Input");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("입력값이 유효하지 않습니다.");
		}

		logger.info("BoardController getBoard() ==========> boardService.getBoard()");
		// Response(응답)
		BoardResponseDTO response = null;

		try {
			response = boardService.getBoard(RequestBoardId);
		} catch (NoSuchElementException e) {
			logger.error("BoardController getBoard() NoSuchElementException Catch   : " + e.getMessage());
			// '게시판'이 '존재' 하지 않아 찾을 수 없으므로, NotFound(404) 반환
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("조회할 게시판이 존재 하지 않습니다.");
		}
		logger.info("BoardController getBoard() <========== boardService.getBoard()");

		logger.info("BoardController getBoard() Success End");
		return ResponseEntity.ok(response);
    }

    // 특정 게시판(boardId) 기준 계층(자식) 구조 조회
    @GetMapping("/{boardId}/hierarchy")
    public ResponseEntity<?> getBoardHierarchyByParent(@PathVariable(name = "boardId") Long boardId) {

    	logger.info("BoardController getBoardHierarchyByParent() Start");

    	if(!BoardValidation.isValidBoardId(boardId)) {
			logger.error("BoardController getBoardHierarchyByParent() 'boardId' Invaild Input");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("입력값이 유효하지 않습니다.");
    	}
  
    	BoardHierarchyResponsetDTO response = null;

    	logger.info("BoardController getBoardHierarchyByParent() ==========> boardService.getBoardHierarchyByParent()");
    	try {
    		response = boardService.getBoardHierarchyByParent(boardId);
		} catch (NoSuchElementException e) {
			logger.error("BoardController getBoardHierarchyByParent() Error : " + e.getMessage());
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		}
    	logger.info("BoardController getBoardHierarchyByParent() <========== boardService.getBoardHierarchyByParent()");

        logger.info("BoardController getBoardHierarchyByParent() Success End");
        return ResponseEntity.ok(response);
    }

    // 전체 게시판 목록 조회 (계층 구조 'X')
    @GetMapping
    public ResponseEntity<?> getParentBoards() {

    	logger.info("BoardController getParentBoards() Start");

    	logger.info("BoardController getParentBoards() ==========> boardService.getParentBoards()");
    	// 부모 게시판
    	List<BoardResponseDTO> response  = null;

    	try {
    		response = boardService.getParentBoards();
		} catch (NoSuchElementException e) {
			logger.error("BoardController getParentBoards NoSuchElementException Error : "+e.getMessage());
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		}

    	logger.info("BoardController getParentBoards() <========== boardService.getParentBoards()");

    	logger.info("BoardController getParentBoards() Success End");
    	return ResponseEntity.ok(response);
    }

    // 전체 게시판 목록 조회 (부모 게시판 + 자식 게시판 계층 구조 'O')
    @GetMapping("/admin/hierarchy")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> getBoardFullHierarchy() {

    	logger.info("BoardController getBoardFullHierarchy() Start");

    	logger.info("BoardController getBoardFullHierarchy() ==========> boardService.getBoardFullHierarchy()");
    	List<BoardHierarchyResponsetDTO> response  = null;
    	try {
			response = boardService.getBoardFullHierarchy();
		} catch (NoSuchElementException e) {
			logger.error("BoardController getBoardFullHierarchy NoSuchElementException Error : "+e.getMessage());
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		}
    	logger.info("BoardController getBoardFullHierarchy() <========== boardService.getBoardFullHierarchy()");

    	if(response.isEmpty()) {
    		logger.error("게시판 계층 구조가 존재하지 않습니다.");
    		return ResponseEntity.status(HttpStatus.NO_CONTENT).body("게시판 계층 구조가 존재하지 않습니다.");
    	}

    	logger.info("BoardController getBoardHierarchy() Success End");
    	return ResponseEntity.ok(response);
    }

    //*************************************************** API START ***************************************************//

}

