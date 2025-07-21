package com.example.demo.repository.post;


import com.example.demo.domain.post.Post;
import com.example.demo.domain.post.postenums.PostStatus;
import com.example.demo.domain.board.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// 공지글 : HTML Header
// 게시글 : HTML body
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

	// 특정 게시판(Board)의 게시글 목록조회 - 페이징 처리(게시판별 조회 + 페이징)
	Page<Post> findByBoard(Board board, Pageable pageable);

	// 게시글 제목으로 검색 한것을 (예: 제목에 키워드 포함), (제목 검색 + 대소문자 무시 + 페이징) 페이징 처리
	Page<Post> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);

	// 게시글 ID로 조회 ('Optional'로 'null' 안전하게 처리)
	Optional<Post> findByPostId(Long postId);

	// 전체 게시판(자유게시판, 유저게시판, 공지게시판)모두를 포함한 공지글(isNotice = true)만 조회
	List<Post> findByIsNoticeTrue();
	// 특정 게시판(공지게시판)에서 공지글(isNotice = true)만 조회 (메인페이지 header)
	List<Post> findByBoardAndIsNoticeTrue(Board board);

	// 삭제된 게시글 제외
	Page<Post> findByStatus(PostStatus status, Pageable pageable);


	//  조합: 게시판 + 상태 + 공지글 제외 (메인페이지 body, 특정페이지)
	Page<Post> findByBoardAndStatusAndIsNoticeFalse(Board board, PostStatus status, Pageable pageable);

}
