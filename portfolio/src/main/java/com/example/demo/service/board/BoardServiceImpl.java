package com.example.demo.service.board;


import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.controller.member.MemberController;
import com.example.demo.domain.board.Board;
import com.example.demo.dto.board.BoardCreateRequestDTO;
import com.example.demo.dto.board.BoardResponseDTO;
import com.example.demo.dto.board.BoardUpdateRequestDTO;
import com.example.demo.repository.board.BoardRepository;
import com.example.demo.validation.string.SafeTrim;

import lombok.RequiredArgsConstructor;


@Service
// 'final' 또는 '@NonNull' 붙은 '필드'들을 대상으로 '생성자 자동 생성' 
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardServiceImpl implements BoardService {

	private final BoardRepository boardRepository;
	private static final Logger logger = LoggerFactory.getLogger(BoardServiceImpl.class);

	//*************************************************** Service START ***************************************************//

	@Override
	@Transactional
	public BoardResponseDTO createBoard(BoardCreateRequestDTO trimBoardCreateRequestDTO) {

		logger.info("BoardServiceImpl createBoard() Start");

		// sortOrder 증감 처리
		Integer sortOrder = boardRepository.findMaxSortOrder();

		// 'sortOrder'가 'null'이면 
		if(sortOrder == null) {
			sortOrder = 0;
		}else {
			sortOrder = sortOrder +1;
		}

		// DTO('Controller' 에서 'trim' 처리 후 'Service'도착)
		String trimBoardName = trimBoardCreateRequestDTO.getName();
		String trimBoardDescription = trimBoardCreateRequestDTO.getDescription();

		// 중복 체크
		if(boardRepository.existsByName(trimBoardName)) {
			logger.warn("BoardServiceImpl createBoard() '첫번쨰 IF문' Error : 이미 존재하는 게시판 입니다.");
			throw new IllegalArgumentException("이미 존재하는 게시판 이름입니다.");
		}

		Board board = new Board();
		board.setName(trimBoardName);
		board.setDescription(trimBoardDescription);
		board.setSortOrder(sortOrder);
		
		// <S extends T> S save(S entity)
		// 'S'는 'T'이거나 'T'의 '자식클래스'
		// 타입 명확하므로 '?'와 달리 '쓰기/읽기' 가능, 반면 '?'는 타입이 불명확해 '읽기'만 가능 
		Board saveBoard = boardRepository.save(board);

		logger.info("BoardServiceImpl createBoard() Success End");
		return BoardResponseDTO.convertToResponseDTO(saveBoard);
	}

	@Override
	@Transactional
	public BoardResponseDTO updateBoard(Long boardId, BoardUpdateRequestDTO boardUpdateRequestDTO) {

		logger.info("BoardServiceImpl updateBoard() Start");

		// 게시판 ID
		Long RequestBoardId = boardId;

		// DTO (After Trim)
		String trimBoardName = boardUpdateRequestDTO.getName();
		String trimBoardDescription = boardUpdateRequestDTO.getDescription();
		Integer boardSortOrder = boardUpdateRequestDTO.getSortOrder();
		boolean boardIsActive = boardUpdateRequestDTO.isActive();

		// 유효성 체크
		Board board = boardRepository.findById(RequestBoardId)
				                     .orElseThrow(() -> new NoSuchElementException("게시판이 존재하지 않습니다."));

		// DB
		String dbTrimBoardName = SafeTrim.safeTrim(board.getName());

		// 게시판 제목 유효성 체크
		if(trimBoardName != null && !trimBoardName.isEmpty()) {
			if(boardRepository.existsByName(trimBoardName) && !dbTrimBoardName.equals(trimBoardName)) {
				throw new IllegalArgumentException("게시판 이름이 중복됩니다.");
			}
			board.setName(trimBoardName);
		}

		// 게시판 설명 유효성 체크
		if(trimBoardDescription != null && !trimBoardDescription.isEmpty()) {
			logger.info("BoardServiceImpl updateBoard() 게시판 설명 유효성 체크");
			board.setDescription(trimBoardDescription);
		}
	
		board.setActive(boardIsActive);

		// 나열 기능 유효성 체크
		if(boardSortOrder != null) {
			logger.info("BoardServiceImpl updateBoard() 나열 기능 유효성 체크");
			board.setSortOrder(boardSortOrder);
		}

		logger.info("BoardServiceImpl updateBoard() Success End");
		return BoardResponseDTO.convertToResponseDTO(board);
	}

	@Override
	@Transactional
	public void deleteBoard(Long boardId) {

		logger.info("BoardServiceImpl deleteBoard() Start");

		//게시판 ID
		Long RequestBoardId = boardId;

		Board board = boardRepository.findById(RequestBoardId)
				                     .orElseThrow(() -> new NoSuchElementException("게시판이 존재하지 않습니다."));

		logger.info("BoardServiceImpl deleteBoard() Success End");
		boardRepository.delete(board);
	}

	// 게시판 단건 조회
	@Override
	public BoardResponseDTO getBoard(Long boardId) {

		logger.info("BoardServiceImpl  getBoard() Start");
		//게시판 ID
		Long RequestBoardId = boardId;

		Board board = boardRepository.findById(RequestBoardId)
				                     .orElseThrow(() -> new NoSuchElementException("게시판이 존재하지 않습니다."));

		logger.info("BoardServiceImpl  getBoard() Success End");
		return BoardResponseDTO.convertToResponseDTO(board);
	}

	@Override
	public List<BoardResponseDTO> getAllBoards() {

		logger.info("BoardServiceImpl  getAllBoards() Start");

		// 'sort_order'필드를 이용하여 게시판 'ASC(오름차순)'으로 정렬 한 후,
		// 전체 게시판 가져오기
		List<Board> boards = boardRepository.findAll(Sort.by(Sort.Direction.ASC,"sortOrder"));

										  // -> 'List'안에 'Board'객체를 'Stream' 변환
		List<BoardResponseDTO> response = boards.stream() 
				 						  // -> 'Stream'안에 있는 'Board'객체를 'BoardResponseDTO'클래스의,
				 						  // 'convertToResponseDTO()'에 의해서 'ResponseDTO' 반환
				 						  // -> 'Board' -> 'ResponseDTO'
			     						  .map(BoardResponseDTO :: convertToResponseDTO) 
			     						  // -> 반환된 'ResponseDTO'객체를 'List<ResponseDTO>'반환
			     						  .collect(Collectors.toList());
		
		logger.info("BoardServiceImpl  getAllBoards() End");
		return response;
		
	}

	//*************************************************** Service END ***************************************************//

}
