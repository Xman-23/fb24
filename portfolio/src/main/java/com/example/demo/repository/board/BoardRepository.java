package com.example.demo.repository.board;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.board.Board;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

	// 게시판 이름으로 조회 (중복 불가 시, 'name' 필드 : 'unique' 제약)
	boolean existsByName (String name);

	// 'sort_order'조회
	// JPQL 쿼리는 엔티티를 기준으로 작성한다.
	// 따라서 테이블명이 아닌 엔티티 클래스명을 사용해야 하며,
	// 엔티티 클래스명이 'Board'라면 대문자 B를 그대로 사용한다.
	// b.sortOrder는 Board 엔티티의 필드명을 가리킨다.
	// 즉, 실제 DB 컬럼명이 'sort_order'여도 엔티티 필드명이 'sortOrder'라면
	// JPQL에서는 'b.sortOrder'로 써야 한다.
	@Query(
			"SELECT MAX(b.sortOrder) "
		  + "  FROM Board b "
		  )
	Integer findMaxSortOrder();

	List<Board> findByParentBoardIsNull();

	// 부모게시판의 ID를 가진 자식게시판들의 ID 조회
	@Query(
			"SELECT b.boardId "
		  + "  FROM Board b "
		  + " WHERE b.parentBoard.boardId = :parentBoardId "
		  )
	List<Long> findChildBoardIds(@Param("parentBoardId")Long parentBoardId);

}
