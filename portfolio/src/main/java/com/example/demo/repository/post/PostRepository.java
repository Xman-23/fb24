package com.example.demo.repository.post;


import com.example.demo.domain.post.Post;

import com.example.demo.domain.post.postenums.PostStatus;
import com.example.demo.domain.board.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

	// 전체 게시판(자유게시판, 유저게시판, 공지게시판)모두를 포함한 공지글(isNotice = true)만 조회
	List<Post> findByIsNoticeTrue();

	// 전체 공지글 (공지 게시판 전용)
	Page<Post> findByBoardAndIsNoticeTrueAndStatus(Board board, PostStatus status, Pageable pageable);

	// 공지 게시판에서 핀으로 고정된 게시글만 조회
	@Query( "SELECT p "
		  + "  FROM Post p "
		  + " WHERE p.board.id = :boardId "
		  + "   AND p.isPinned = true "
		  + "   AND p.status = 'ACTIVE'"
		  + " ORDER BY p.createdAt DESC"
		  )
	List<Post> findPinnedNoticesByBoard(@Param("boardId") Long boardId, Pageable pageable);

	// 게시글 상태가 ACTIVE인 게시글만 조회 (전체 게시글 목록 중 ACTIVE 상태만 필터링) // 사용
	Page<Post> findByStatus(PostStatus status, Pageable pageable);

	// 게시판별 게시글 중 ACTIVE 상태이고 공지가 아닌 게시글만 조회 (메인페이지 body, 특정 게시판 페이징) 사용
	Page<Post> findByBoardAndStatusAndIsNoticeFalse(Board board, PostStatus status, Pageable pageable);

	// 제목 또는 내용에 키워드를 포함한 게시글 검색 (ACTIVE 상태만 검색, 대소문자 무시, 페이징 처리) 사용
	Page<Post> findByStatusAndTitleContainingIgnoreCaseOrStatusAndContentContainingIgnoreCase(
		    PostStatus status1, String keyword1,
		    PostStatus status2, String keyword2,
		    Pageable pageable
	);

	// 인기순 정렬 (좋아요 + 댓글 수를 기준으로 내림차순으로 정렬한 후 생성일자로 다시한번 내림차순)
	@Query ("SELECT p " // 마지막에 한칸씩 공백
		   + "  FROM Post p " // 마지막에 한칸씩 공백
		   + " WHERE p.status = 'ACTIVE' " //마지칵에 한칸씩 공백
		   + " ORDER BY (p.likeCount + SIZE(p.comments)) DESC, p.createdAt DESC"
		   )
	Page<Post> findPopularPosts(Pageable pageable);
	
	// 최신순 정렬 ('ACTIVE' 상태인 게시글을 최신 생성일자 내림차순으로 정렬)
	Page<Post> findByStatusOrderByCreatedAtDesc(PostStatus status, Pageable pageable);

	// 'ACTIVE 게시글'에서 '작성자ID(memberId)'를 검색 하여 페이징 처리 
	Page<Post> findByAuthorIdAndStatus(Long authorId, PostStatus status, Pageable pageable);
	
	// 3개의 핀으로 설정된 공지 게시글 모든 게시판에 보여주기
	List<Post> findTop3ByBoardAndIsPinnedTrueAndIsNoticeTrueAndStatusOrderByCreatedAtDesc (Board board,
			                                                                               PostStatus status);
	
	@Modifying
	@Query("UPDATE Post p "
		  + "SET p.viewCount = p.viewCount + 1 "
		  + "WHERE p.postId = :postId"
		  )
	int incrementViewCount(@Param("postId") Long postId);

	@Query("SELECT p "
		  + "FROM Post p "
		  + "WHERE p.board.boardId IN :boardIds "
		  + "AND p.status = 'ACTIVE' "
		  + "AND p.isNotice = false"
		  )
	Page<Post> findActiveNonNoticePostsByBoardIds(@Param("boardIds")List<Long> boardIds, Pageable pageable);

}
