package com.example.demo.repository.post;


import com.example.demo.domain.post.Post;

import com.example.demo.domain.post.postenums.PostStatus;
import com.example.demo.domain.board.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

	// 전체 공지글 (공지 게시판 전용)
	@Query( "SELECT p "
		  + "  FROM Post p "
		  + " WHERE p.board.boardId = :boardId "
		  + "   AND p.isNotice = true "
		  + "   AND p.status = :status"
		  )
	Page<Post> findNoticePosts(@Param("boardId") Long boardId, 
			                   @Param("status") PostStatus status, 
			                   Pageable pageable);

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
	@Query( "SELECT p " 
		  + "  FROM Post p " 
		  + " WHERE p.status = 'ACTIVE' " 
		  + "   AND (" 
		  + "        LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " 
		  +          "OR "
		  +          "LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))" 
		  +        ")")
	Page<Post> searchByKeyword(@Param("keyword")String keyword, Pageable pageable);

	// 자식게시판 인기순 정렬
	@Query( "SELECT p "
		  + "  FROM Post p "
		  + " WHERE p.board = :board "
		  + "   AND p.status = 'ACTIVE' "
		  + "   AND p.isNotice = false "
		  + " ORDER BY (SELECT COUNT(r)"
		  + "             FROM PostReaction r"
		  + "            WHERE r.post = p"
		  + "              AND r.reactionType = 'LIKE'"
		  + "          ) DESC, "
		  + "          p.createdAt DESC"
		  )
	Page<Post> findPopularPostsByBoard(@Param("board") Board board, Pageable pageable);

	// 자식게시판 최신순 정렬
	Page<Post> findByBoardAndStatusAndIsNoticeFalseOrderByCreatedAtDesc(Board board, 
																		PostStatus status, 
																		Pageable pageable);
	
	// 부모게시판 최신순 정렬
	@Query( "SELECT p "
		  + "  FROM Post p "
		  + " WHERE p.board.boardId  IN (:boardIds) "
		  + "   AND p.status = 'ACTIVE' "
		  + "   AND p.isNotice  = false "
		  + " ORDER BY p.createdAt DESC")
	Page<Post> findByBoardIds(@Param("boardIds") List<Long> boardIds, Pageable pageable);

	// 부모게시판 인기순 정렬
	@Query( "SELECT p "
		  + "  FROM Post p "
		  + " WHERE p.board.boardId IN (:boardIds) "
		  + "   AND p.status = 'ACTIVE' "
		  + "   AND p.isNotice = false "
		  + " ORDER BY (SELECT COUNT(r) "
		  + "             FROM PostReaction r "
		  + "            WHERE r.post = p"
		  + "			   AND r.reactionType = 'LIKE'"
		  + "          )DESC,"
		  + "          p.createdAt DESC"
		  )
	Page<Post> findActiveNonNoticePostsByBoardIdsOrderByLikesDescCreatedAtDesc(@Param("boardIds") List<Long> boardIds, Pageable pageable);

	// 'ACTIVE 게시글'에서 '작성자ID(memberId)'를 검색 하여 페이징 처리 
	Page<Post> findByAuthorIdAndStatus(Long authorId, PostStatus status, Pageable pageable);
	
	// 3개의 핀으로 설정된 공지 게시글 모든 게시판에 보여주기
	List<Post> findTop3ByBoardAndIsPinnedTrueAndIsNoticeTrueAndStatusOrderByCreatedAtDesc (Board board,
			                                                                               PostStatus status);
	// 조회수 증가
	@Modifying
	@Query( "UPDATE Post p "
		  + "   SET p.viewCount = p.viewCount + 1 "
		  + "WHERE p.postId = :postId"
		  )
	int incrementViewCount(@Param("postId") Long postId);

	// 자식 게시판에서 공지글이 아닌 'ACTIVE'인 게시글 모두 조회 
	@Query( "SELECT p "
		  + "  FROM Post p "
		  + " WHERE p.board.boardId IN (:boardIds) "
		  + "  AND p.status = 'ACTIVE' "
		  + "  AND p.isNotice = false"
		  )
	Page<Post> findActiveNonNoticePostsByBoardIds(@Param("boardIds")List<Long> boardIds, Pageable pageable);

	// 좋아요 게시글 뽑기
	@Query( "SELECT p "
		  + "  FROM Post p "
		  + " WHERE p.board.boardId IN (:boardIds) "
		  + "   AND p.isNotice = false "
		  + "   AND p.isPinned = false "
		  + "   AND p.status = 'ACTIVE' "
		  + "   AND p.likeCount >= :likeThreshold "
		  + "   AND p.createdAt >= :recentThreshold "
		  + " ORDER BY p.likeCount DESC, p.createdAt DESC "
		  )
	List<Post> findTopPopularPostsByBoards(@Param("boardIds")List<Long> boardIds,
										   @Param("likeThreshold") int likeThreshold,
										   @Param("recentThreshold") LocalDateTime recentThreshold,
										   Pageable pageable);

}
