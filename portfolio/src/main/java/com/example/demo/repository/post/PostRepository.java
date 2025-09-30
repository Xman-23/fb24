package com.example.demo.repository.post;


import com.example.demo.domain.post.Post;


import com.example.demo.domain.post.postenums.PostStatus;
import com.example.demo.domain.member.Member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    @Query(
    		"SELECT p "
    	  + "  FROM Post p "
    	  + " WHERE p.author.id = :authorId "
    	  + "   AND p.status = :status "
    	  + " ORDER BY p.createdAt DESC "
    	  )
    Page<Post> findByAuthor(@Param("authorId") Long authorId,
            				@Param("status") PostStatus status,
            				Pageable pageable);
	
	// 상단 공지 게시글 3개뽑기
	@Query(
			"SELECT p "
		  + "  FROM Post p "
		  + " WHERE p.board.boardId = 1 "
		  + "   AND p.status = 'ACTIVE' "
		  + "   AND p.isNotice = true "
		  + "   AND p.isPinned = true "
		  + " ORDER BY p.createdAt DESC "
		  )
	List<Post> findTop3FixedNotices(Pageable pageable);

	// 전체 공지글 (공지 게시판 전용)
	@Query(
			"SELECT p "
		  + "  FROM Post p "
		  + " WHERE p.board.boardId = 1 "
		  + "   AND p.status = 'ACTIVE' "
		  + "   AND p.isNotice = true "
		  + " ORDER BY p.createdAt DESC "
		  )
	Page<Post> findNoticePosts(Pageable pageable);

	// 공지게시판 제목/본문 키워드 검색
	@Query(
	      "SELECT p "
	    + "  FROM Post p "
	    + " WHERE p.board.boardId = 1 "
	    + "   AND p.status = 'ACTIVE' "
	    + "   AND p.isNotice = true "
	    + "   AND ("
	    + "        p.title LIKE %:keyword% "
	    + "    OR  p.content LIKE %:keyword% "
	    + "       ) "
	    + " ORDER BY p.createdAt DESC "
	)
	Page<Post> searchNoticePostsByKeyword(@Param("keyword") String keyword, Pageable pageable);

	// 공지게시판 제목 자동완성 (최대 10개)
	@Query(
	      "SELECT p.title "
	    + "  FROM Post p " 
	    + " WHERE p.board.boardId = 1 "
	    + "   AND p.status = 'ACTIVE' "
	    + "   AND p.isNotice = true "
	    + "   AND ("
	    + "        p.title LIKE %:keyword% "
	    + "    OR  p.content LIKE %:keyword% "
	    + "       ) "
	    + " ORDER BY p.createdAt DESC "
	)
	Page<String> autoCompleteNoticeTitles(@Param("keyword") String keyword, Pageable pageable);
	
	// 공지게시판 제목 게시글 조회
	@Query(
		  "SELECT p "
	    + "  FROM Post p " 
	    + " WHERE p.board.boardId = 1 "
	    + "   AND p.status = 'ACTIVE' "
	    + "   AND p.isNotice = true "
	    + "   AND p.title = :title "
	    + " ORDER BY p.createdAt DESC "
	)
	Page<Post> autoCompleteSearchNoticeTitles(@Param("title")String title, Pageable pageable);

	// 공지게시판 작성자 닉네임 검색
	@Query(
	      "SELECT p "
	    + "  FROM Post p "
	    + " WHERE p.board.boardId = 1 "
	    + "   AND p.status = 'ACTIVE' "
	    + "   AND p.isNotice = true "
	    + "   AND p.author = :member "
	    + " ORDER BY p.createdAt DESC "
	)
	Page<Post> searchNoticePostsByAuthor(@Param("member") Member member, Pageable pageable);

	// 통합검색 제목 또는 내용에 키워드를 포함한 게시글 검색 (ACTIVE 상태만 검색, 대소문자 무시, 페이징 처리) 사용
	@Query( "SELECT p " 
		  + "  FROM Post p " 
		  + " WHERE p.status = 'ACTIVE' "
		  + "   AND p.isNotice = false " 
		  + "   AND (" 
		  + "         LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " 
		  + "    OR "
		  + "         LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))" 
		  + "       ) "
		  + " ORDER BY p.createdAt DESC"
		  )
	Page<Post> searchByKeyword(@Param("keyword")String keyword, Pageable pageable);

    // 통합검색 자동완성용: 제목 LIKE 검색, ACTIVE 상태, 대소문자 무시, 최대 10개
    @Query(
    		"SELECT p.title "
    	  + "FROM Post p " 
          + "WHERE p.status = 'ACTIVE' "
          + "  AND p.isNotice = false "
          + "  AND LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) "
          + "ORDER BY p.createdAt DESC"
          )
    Page<String> searchTitlesByKeyword(@Param("keyword") String keyword, Pageable pageable);

	// 통합검색 자동완성 제목 게시글 조회
	@Query(
		    "SELECT p "
		  + "  FROM Post p "
		  + " WHERE p.status = 'ACTIVE' "
		  + "   AND p.isNotice = false "
		  + "   AND p.title = :title "
		  + " ORDER BY p.createdAt DESC"
		)
	Page<Post> autoSearchByKeyword(@Param("title")String title, Pageable pageable);

	// 통합검색 'ACTIVE 게시글'에서 '작성자ID(memberId)'를 검색 하여 페이징 처리
	@Query(
			"SELECT p "
		  + "  FROM Post p "
		  + " WHERE p.author = :member "
		  + "   AND p.isNotice = false "
		  + "   AND p.status = :status "
		  + " ORDER BY p.createdAt DESC "
		  )
	Page<Post> findByAuthorAndStatus (@Param("member") Member member, 
			                          @Param("status") PostStatus status, 
			                          Pageable pageable);

	// 자식게시판 최신순 정렬
    @Query(value = 
    		"SELECT * "
    	  + "  FROM post p "
    	  + " WHERE p.board_id = :boardId "
    	  + "   AND p.status = 'ACTIVE' "
    	  + "   AND p.is_notice = false "
    	  + "  ORDER BY p.created_at DESC ",
    	  countQuery =
    	  	"SELECT COUNT(*) "
    	  + "  FROM post p "
    	  + " WHERE p.board_id = :boardId "
    	  + "   AND p.status = 'ACTIVE' "
    	  + "   AND p.is_notice = false",
    	  nativeQuery = true)
	Page<Post> findPostsByBoardLatest(@Param("boardId") Long boardId,  Pageable pageable);

    // 자식게시판 인기순 정렬
    @Query(value = 
            "SELECT p.* "
          + "  FROM post p "
          + "  LEFT JOIN ( " 
          + "             SELECT post_id, "
          + "                    COUNT(*) AS like_count "
          + "               FROM post_reaction "
          + "              WHERE reaction_type = 'LIKE' "
          + "              GROUP BY post_id "
          + "            ) r ON r.post_id = p.post_id "
          + " WHERE p.board_id = :boardId "
          + "   AND p.status = 'ACTIVE' "
          + "   AND p.is_notice = false "
          + "ORDER BY COALESCE(r.like_count, 0) DESC, p.created_at DESC ",
            countQuery = 
            "SELECT COUNT(*) "
          + "  FROM post p "
          + "  LEFT JOIN ( " 
          + "             SELECT post_id, "
          + "                    COUNT(*) AS like_count "
          + "               FROM post_reaction "
          + "              WHERE reaction_type = 'LIKE' "
          + "              GROUP BY post_id "
          + "            ) r ON r.post_id = p.post_id "
          + " WHERE p.board_id = :boardId "
          + "   AND p.status = 'ACTIVE' "
          + "   AND p.is_notice = false ",
            nativeQuery = true
          )
	Page<Post> findPopularPosts(@Param("boardId") Long boardId, Pageable pageable);

    // 자식게시판 실시간 검색: 제목만 조회, 생성일 내림차순
    @Query(value = 
             "SELECT p.title "
           + "  FROM post p "
           + " WHERE p.board_id = :boardId "
           + "   AND p.status = 'ACTIVE' "
           + "   AND p.is_notice = false "
           + "   AND LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) "
           + " ORDER BY p.created_at DESC",
           countQuery =
             "SELECT COUNT(*) "
           + "  FROM post p "
           + " WHERE p.board_id = :boardId "
           + "   AND p.status = 'ACTIVE' "
           + "   AND p.is_notice = false "
           + "   AND LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))",
           nativeQuery = true)
    Page<String> autocompleteBoardNormalPosts(@Param("boardId") Long boardId,
                                              @Param("keyword") String keyword,
                                              Pageable pageable);

    // 자식게시판 실시간 검색: 제목  게시글만 조회
    @Query(value = 
             "SELECT p.* "
           + "  FROM post p "
           + " WHERE p.board_id = :boardId "
           + "   AND p.status = 'ACTIVE' "
           + "   AND p.is_notice = false "
           + "   AND p.title = :title "
           + " ORDER BY p.created_at DESC",
           countQuery =
             "SELECT COUNT(*) "
           + "  FROM post p "
           + " WHERE p.board_id = :boardId "
           + "   AND p.status = 'ACTIVE' "
           + "   AND p.is_notice = false "
           + "   AND p.title = :title ",
           nativeQuery = true)
    Page<Post> autocompleteSearchBoardNormalPosts(@Param("boardId") Long boardId,
                                                  @Param("title") String title,
                                                  Pageable pageable);

	// 자식게시판 키워드 검색
    @Query(value = 
    		"SELECT * "
    	  + "  FROM post p"
    	  + " WHERE p.board_id = :boardId "
    	  + "   AND p.status = 'ACTIVE' "
    	  + "   AND p.is_notice = false "
    	  + "   AND ( "
    	  + "        LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) "
    	  + "    OR "
    	  + "        LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))"
    	  + "       ) "
    	  + "  ORDER BY created_at DESC ",
    	  countQuery =
    	  	"SELECT COUNT(*) "
    	  + "  FROM post p"
    	  + " WHERE p.board_id = :boardId "
    	  + "   AND p.status = 'ACTIVE' "
    	  + "   AND p.is_notice = false "
    	  + "   AND ( "
    	  + "        LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) "
    	  + "    OR "
    	  + "        LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))"
    	  + "       ) ",
    	  nativeQuery = true)
	Page<Post> searchPostsByKeyword(@Param("boardId") Long boardId,  
			                        @Param("keyword") String keyword,
			                        Pageable pageable);

	// 자식게시판 닉네임 검색
    @Query(value = 
    		"SELECT * "
    	  + "  FROM post p"
    	  + " WHERE p.board_id = :boardId "
    	  + "   AND p.status = 'ACTIVE' "
    	  + "   AND p.is_notice = false "
    	  + "   AND p.author_id  = :authorId "
    	  + "  ORDER BY created_at DESC ",
    	  countQuery =
    	  	"SELECT COUNT(*) "
    	  + "  FROM post p"
    	  + " WHERE p.board_id = :boardId "
    	  + "   AND p.status = 'ACTIVE' "
    	  + "   AND p.is_notice = false "
    	  + "   AND p.author_id = :authorId ",
    	  nativeQuery = true)
	Page<Post> searchPostsByAuthor(@Param("boardId") Long boardId,  
			                       @Param("authorId") Long authorId,
			                       Pageable pageable);

	/* 조회수 증가
	   @Modifying(clearAutomatically = true)
	   JPQL UPDATE 후 1차 캐시를 자동으로 비워줘서,
	   다음 조회 시 DB에서 최신 데이터를 가져오도록 함
	*/
	@Modifying(clearAutomatically = true)
	@Transactional
	@Query( "UPDATE Post p "
		  + "   SET p.viewCount = p.viewCount + 1 "
		  + " WHERE p.postId = :postId "
		  )
	int incrementViewCount(@Param("postId") Long postId);

	// 자식 게시판 상단 인기글 뽑기 3개 뽑기 (총 갯수를 뽑는게 아니므로 = 'List')
	//'nativeSQL'사용하므로, @PageableDefault 에서 페이징 조건(size page)만 명시, 정렬(sort, direction)은 명시해도 적용이 되지않음
	@Query(value = 
			"SELECT p.* "
		  + "  FROM post p "
		  + "  LEFT JOIN ( "
		  + "			  SELECT pr.post_id, "
		  + "                    SUM(CASE "
		  + "							 WHEN pr.reaction_type = 'LIKE' THEN 1 "
		  + "                            WHEN pr.reaction_type = 'DISLIKE' THEN -1 "
		  + "                            ELSE 0 "
		  + "						  END) AS net_like_count,"
		  + "                    SUM(CASE WHEN pr.reaction_type = 'LIKE' THEN 1 ELSE 0 END) AS like_count "
		  + "               FROM post_reaction pr "
		  + "              GROUP BY pr.post_id "
		  + "            ) AS pr_summary "
		  + "    ON p.post_id = pr_summary.post_id "
		  + " WHERE p.board_id = :boardId "
		  + "   AND p.is_notice = false "
		  + "   AND p.status = 'ACTIVE' "
		  + "   AND p.created_at >= :recentThreshold "
		  + "   AND COALESCE(pr_summary.like_count, 0) >= :likeThreshold "
		  + "   AND pr_summary.net_like_count >= :netLikeThreshold "
		  + " ORDER BY COALESCE(pr_summary.like_count, 0) DESC, "
		  + "          p.created_at DESC "
		  + " LIMIT :limit ", 
		  nativeQuery = true)
	List<Post> findTopPostsByBoardWithNetLikes(@Param("boardId") Long boardId,
											   @Param("likeThreshold") int likeThreshold,
											   @Param("netLikeThreshold") int netLikeThreshold,
											   @Param("recentThreshold") LocalDateTime recentThreshold,
											   @Param("limit") int limit);

	/* 좋아요 60일 기준으로 가중치 계산 
	 한 페이지당 받을 데이터(content)와 데이터의 총 갯수 (totalElements) 구하기
	 파라미터로 'Pageable'을 받을시에 개발자가 직접 정해준 'size'에 의해 'totlaPage' 구해짐
	 */
	//(좋아요순 먼저 정렬후, 최신순 정렬)
	@Query(value = 
		    "SELECT p.* "
		  + "  FROM post p "
		  + "  LEFT JOIN ("
		  + "              SELECT pr.post_id, "
		  + "                     SUM(CASE "
		  + "							  WHEN pr.reaction_type = 'LIKE' THEN 1 "
		  + "							  ELSE 0 "
		  + "                          END) AS like_count, "
		  + "                     SUM(CASE "
		  + "                             WHEN pr.reaction_type = 'LIKE' THEN 1 "
		  + "                             WHEN pr.reaction_type = 'DISLIKE' THEN -1 "
		  + "                             ELSE 0 "
		  + "                          END) AS net_like_count "
		  + "                FROM post_reaction pr "
		  + "               GROUP BY pr.post_id "
		  + "            ) AS pr_summary ON p.post_id = pr_summary.post_id "
		  + " WHERE p.board_id IN (:childBoardIds) "
		  + "   AND p.is_notice = false "
		  + "   AND p.status = 'ACTIVE' "
		  + "   AND p.created_at >= DATE_SUB(NOW(), INTERVAL :dayLimit DAY) "
		  + "   AND COALESCE(pr_summary.like_count, 0) >= :likeThreshold "
		  + "   AND pr_summary.net_like_count >= :netLikeThreshold "
		  + " ORDER BY (COALESCE(pr_summary.like_count, 0) * 1.0 + GREATEST(0, :dayLimit - DATEDIFF(NOW(), p.created_at))) DESC, "
		  + "          p.created_at DESC ",
		  countQuery = 
		    "SELECT COUNT(*) "
		  + "  FROM post p "
		  + "  LEFT JOIN ("
		  + "              SELECT pr.post_id, "
		  + "                     SUM(CASE "
		  + "                             WHEN pr.reaction_type = 'LIKE' THEN 1 "
		  + "                             ELSE 0 "
		  + "                          END) AS like_count, "
		  + "                     SUM(CASE "
		  + "                              WHEN pr.reaction_type = 'LIKE' THEN 1 "
		  + "                              WHEN pr.reaction_type = 'DISLIKE' THEN -1 "
		  + "                              ELSE 0 "
		  + "                          END) AS net_like_count "
		  + "                FROM post_reaction pr "
		  + "               GROUP BY pr.post_id "
		  + "            ) AS pr_summary ON p.post_id = pr_summary.post_id "
		  + " WHERE p.board_id IN (:childBoardIds) "
		  + "   AND p.is_notice = false "
		  + "   AND p.status = 'ACTIVE' "
		  + "   AND p.created_at >= DATE_SUB(NOW(), INTERVAL :dayLimit DAY) "
		  + "   AND COALESCE(pr_summary.like_count, 0) >= :likeThreshold "
		  + "   AND pr_summary.net_like_count >= :netLikeThreshold ",
		  nativeQuery = true)
	Page<Post> findPopularPostsByWeightedScore(@Param("childBoardIds") List<Long> childBoardIds,
			                                   @Param("likeThreshold") int likeThreshold,
			                                   @Param("netLikeThreshold") int netLikeThreshold,
			                                   @Param("dayLimit") int dayLimit,
			                                   Pageable pageable);
	
	// 부모게시판 키워드 검색 (최신순 먼저 정렬후, 좋아요순 정렬)
	@Query(value = 
		    "SELECT p.* "
		  + "  FROM post p "
		  + "  LEFT JOIN ("
		  + "              SELECT pr.post_id, "
		  + "                     SUM(CASE "
		  + "							  WHEN pr.reaction_type = 'LIKE' THEN 1 "
		  + "							  ELSE 0 "
		  + "                          END) AS like_count, "
		  + "                     SUM(CASE "
		  + "                             WHEN pr.reaction_type = 'LIKE' THEN 1 "
		  + "                             WHEN pr.reaction_type = 'DISLIKE' THEN -1 "
		  + "                             ELSE 0 "
		  + "                          END) AS net_like_count "
		  + "                FROM post_reaction pr "
		  + "               GROUP BY pr.post_id "
		  + "            ) AS pr_summary ON p.post_id = pr_summary.post_id "
		  + " WHERE p.board_id IN (:childBoardIds) "
		  + "   AND p.is_notice = false "
		  + "   AND p.status = 'ACTIVE' "
		  + "   AND p.created_at >= DATE_SUB(NOW(), INTERVAL :dayLimit DAY) "
		  + "   AND COALESCE(pr_summary.like_count, 0) >= :likeThreshold "
		  + "   AND pr_summary.net_like_count >= :netLikeThreshold "
		  + "   AND ( "
		  + "        LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) "
		  + "    OR "
		  + "        LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))"
		  + "       ) "
		  + " ORDER BY p.created_at DESC, "
		  + "          (COALESCE(pr_summary.like_count, 0) * 1.0 + GREATEST(0, :dayLimit - DATEDIFF(NOW(), p.created_at))) DESC ",
		  countQuery = 
		    "SELECT COUNT(*) "
		  + "  FROM post p "
		  + "  LEFT JOIN ("
		  + "              SELECT pr.post_id, "
		  + "                     SUM(CASE "
		  + "                             WHEN pr.reaction_type = 'LIKE' THEN 1 "
		  + "                             ELSE 0 "
		  + "                          END) AS like_count, "
		  + "                     SUM(CASE "
		  + "                              WHEN pr.reaction_type = 'LIKE' THEN 1 "
		  + "                              WHEN pr.reaction_type = 'DISLIKE' THEN -1 "
		  + "                              ELSE 0 "
		  + "                          END) AS net_like_count "
		  + "                FROM post_reaction pr "
		  + "               GROUP BY pr.post_id "
		  + "            ) AS pr_summary ON p.post_id = pr_summary.post_id "
		  + " WHERE p.board_id IN (:childBoardIds) "
		  + "   AND p.is_notice = false "
		  + "   AND p.status = 'ACTIVE' "
		  + "   AND p.created_at >= DATE_SUB(NOW(), INTERVAL :dayLimit DAY) "
		  + "   AND COALESCE(pr_summary.like_count, 0) >= :likeThreshold "
		  + "   AND pr_summary.net_like_count >= :netLikeThreshold "
		  + "   AND ( "
		  + "        LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) "
		  + "    OR "
		  + "        LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))"
		  + "       ) ",
		  nativeQuery = true)
	Page<Post> searchParentBoardPosts(@Param("childBoardIds") List<Long> childBoardIds,
									  @Param("keyword") String keyword,
									  @Param("likeThreshold") int likeThreshold,
									  @Param("netLikeThreshold") int netLikeThreshold,
									  @Param("dayLimit") int dayLimit,
									  Pageable pageable);

	// 부모게시판 실시간 검색 (최신순 먼저 정렬후, 좋아요순 정렬)
	@Query(value = 
		    "SELECT p.title "
		  + "  FROM post p "
		  + "  LEFT JOIN ("
		  + "              SELECT pr.post_id, "
		  + "                     SUM(CASE "
		  + "							  WHEN pr.reaction_type = 'LIKE' THEN 1 "
		  + "							  ELSE 0 "
		  + "                          END) AS like_count, "
		  + "                     SUM(CASE "
		  + "                             WHEN pr.reaction_type = 'LIKE' THEN 1 "
		  + "                             WHEN pr.reaction_type = 'DISLIKE' THEN -1 "
		  + "                             ELSE 0 "
		  + "                          END) AS net_like_count "
		  + "                FROM post_reaction pr "
		  + "               GROUP BY pr.post_id "
		  + "            ) AS pr_summary ON p.post_id = pr_summary.post_id "
		  + " WHERE p.board_id IN (:childBoardIds) "
		  + "   AND p.is_notice = false "
		  + "   AND p.status = 'ACTIVE' "
		  + "   AND p.created_at >= DATE_SUB(NOW(), INTERVAL :dayLimit DAY) "
		  + "   AND COALESCE(pr_summary.like_count, 0) >= :likeThreshold "
		  + "   AND pr_summary.net_like_count >= :netLikeThreshold "
		  + "   AND LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) "
		  + " ORDER BY p.created_at DESC, "
		  + "          (COALESCE(pr_summary.like_count, 0) * 1.0 + GREATEST(0, :dayLimit - DATEDIFF(NOW(), p.created_at))) DESC ",
		  countQuery = 
		    "SELECT COUNT(*) "
		  + "  FROM post p "
		  + "  LEFT JOIN ("
		  + "              SELECT pr.post_id, "
		  + "                     SUM(CASE "
		  + "                             WHEN pr.reaction_type = 'LIKE' THEN 1 "
		  + "                             ELSE 0 "
		  + "                          END) AS like_count, "
		  + "                     SUM(CASE "
		  + "                              WHEN pr.reaction_type = 'LIKE' THEN 1 "
		  + "                              WHEN pr.reaction_type = 'DISLIKE' THEN -1 "
		  + "                              ELSE 0 "
		  + "                          END) AS net_like_count "
		  + "                FROM post_reaction pr "
		  + "               GROUP BY pr.post_id "
		  + "            ) AS pr_summary ON p.post_id = pr_summary.post_id "
		  + " WHERE p.board_id IN (:childBoardIds) "
		  + "   AND p.is_notice = false "
		  + "   AND p.status = 'ACTIVE' "
		  + "   AND p.created_at >= DATE_SUB(NOW(), INTERVAL :dayLimit DAY) "
		  + "   AND COALESCE(pr_summary.like_count, 0) >= :likeThreshold "
		  + "   AND pr_summary.net_like_count >= :netLikeThreshold "
		  + "   AND LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ",
		  nativeQuery = true)
	Page<String> autocompleteParentBoardPopularPosts (@Param("childBoardIds") List<Long> childBoardIds,
	         										  @Param("keyword") String keyword,
	         										  @Param("dayLimit") int dayLimit,
	         										  @Param("likeThreshold") int likeThreshold,
	         										  @Param("netLikeThreshold") int netLikeThreshold,
	         										  Pageable pageable);
	
	// 부모게시판 실시간 검색 이동 (최신순 먼저 정렬후, 좋아요순 정렬)
	@Query(value = 
		    "SELECT p.* "
		  + "  FROM post p "
		  + "  LEFT JOIN ("
		  + "              SELECT pr.post_id, "
		  + "                     SUM(CASE "
		  + "							  WHEN pr.reaction_type = 'LIKE' THEN 1 "
		  + "							  ELSE 0 "
		  + "                          END) AS like_count, "
		  + "                     SUM(CASE "
		  + "                             WHEN pr.reaction_type = 'LIKE' THEN 1 "
		  + "                             WHEN pr.reaction_type = 'DISLIKE' THEN -1 "
		  + "                             ELSE 0 "
		  + "                          END) AS net_like_count "
		  + "                FROM post_reaction pr "
		  + "               GROUP BY pr.post_id "
		  + "            ) AS pr_summary ON p.post_id = pr_summary.post_id "
		  + " WHERE p.board_id IN (:childBoardIds) "
		  + "   AND p.is_notice = false "
		  + "   AND p.status = 'ACTIVE' "
		  + "   AND p.created_at >= DATE_SUB(NOW(), INTERVAL :dayLimit DAY) "
		  + "   AND COALESCE(pr_summary.like_count, 0) >= :likeThreshold "
		  + "   AND pr_summary.net_like_count >= :netLikeThreshold "
		  + "   AND p.title = :title "
		  + " ORDER BY p.created_at DESC, "
		  + "          (COALESCE(pr_summary.like_count, 0) * 1.0 + GREATEST(0, :dayLimit - DATEDIFF(NOW(), p.created_at))) DESC ",
		  countQuery = 
		    "SELECT COUNT(*) "
		  + "  FROM post p "
		  + "  LEFT JOIN ("
		  + "              SELECT pr.post_id, "
		  + "                     SUM(CASE "
		  + "                             WHEN pr.reaction_type = 'LIKE' THEN 1 "
		  + "                             ELSE 0 "
		  + "                          END) AS like_count, "
		  + "                     SUM(CASE "
		  + "                              WHEN pr.reaction_type = 'LIKE' THEN 1 "
		  + "                              WHEN pr.reaction_type = 'DISLIKE' THEN -1 "
		  + "                              ELSE 0 "
		  + "                          END) AS net_like_count "
		  + "                FROM post_reaction pr "
		  + "               GROUP BY pr.post_id "
		  + "            ) AS pr_summary ON p.post_id = pr_summary.post_id "
		  + " WHERE p.board_id IN (:childBoardIds) "
		  + "   AND p.is_notice = false "
		  + "   AND p.status = 'ACTIVE' "
		  + "   AND p.created_at >= DATE_SUB(NOW(), INTERVAL :dayLimit DAY) "
		  + "   AND COALESCE(pr_summary.like_count, 0) >= :likeThreshold "
		  + "   AND pr_summary.net_like_count >= :netLikeThreshold "
		  + "   AND p.title = :title ",
		  nativeQuery = true)
	Page<Post> autocompleteSearchParentBoardPopularPosts (@Param("childBoardIds") List<Long> childBoardIds,
	         										      @Param("title") String title,
	         										      @Param("dayLimit") int dayLimit,
	         										      @Param("likeThreshold") int likeThreshold,
	         										      @Param("netLikeThreshold") int netLikeThreshold,
	         										      Pageable pageable);

	// 부모게시판 작성자 검색 (최신순 먼저 정렬후, 좋아요순 정렬)
	@Query(value = 
		    "SELECT p.* "
		  + "  FROM post p "
		  + "  LEFT JOIN ("
		  + "              SELECT pr.post_id, "
		  + "                     SUM(CASE "
		  + "							  WHEN pr.reaction_type = 'LIKE' THEN 1 "
		  + "							  ELSE 0 "
		  + "                          END) AS like_count, "
		  + "                     SUM(CASE "
		  + "                             WHEN pr.reaction_type = 'LIKE' THEN 1 "
		  + "                             WHEN pr.reaction_type = 'DISLIKE' THEN -1 "
		  + "                             ELSE 0 "
		  + "                          END) AS net_like_count "
		  + "                FROM post_reaction pr "
		  + "               GROUP BY pr.post_id "
		  + "            ) AS pr_summary ON p.post_id = pr_summary.post_id "
		  + " WHERE p.board_id IN (:childBoardIds) "
		  + "   AND p.is_notice = false "
		  + "   AND p.status = 'ACTIVE' "
		  + "   AND p.created_at >= DATE_SUB(NOW(), INTERVAL :dayLimit DAY) "
		  + "   AND COALESCE(pr_summary.like_count, 0) >= :likeThreshold "
		  + "   AND pr_summary.net_like_count >= :netLikeThreshold "
		  + "   AND p.author_id = :authorId"
		  + " ORDER BY p.created_at DESC, "
		  + "          (COALESCE(pr_summary.like_count, 0) * 1.0 + GREATEST(0, :dayLimit - DATEDIFF(NOW(), p.created_at))) DESC ",
		  countQuery = 
		    "SELECT COUNT(*) "
		  + "  FROM post p "
		  + "  LEFT JOIN ("
		  + "              SELECT pr.post_id, "
		  + "                     SUM(CASE "
		  + "                             WHEN pr.reaction_type = 'LIKE' THEN 1 "
		  + "                             ELSE 0 "
		  + "                          END) AS like_count, "
		  + "                     SUM(CASE "
		  + "                              WHEN pr.reaction_type = 'LIKE' THEN 1 "
		  + "                              WHEN pr.reaction_type = 'DISLIKE' THEN -1 "
		  + "                              ELSE 0 "
		  + "                          END) AS net_like_count "
		  + "                FROM post_reaction pr "
		  + "               GROUP BY pr.post_id "
		  + "            ) AS pr_summary ON p.post_id = pr_summary.post_id "
		  + " WHERE p.board_id IN (:childBoardIds) "
		  + "   AND p.is_notice = false "
		  + "   AND p.status = 'ACTIVE' "
		  + "   AND p.created_at >= DATE_SUB(NOW(), INTERVAL :dayLimit DAY) "
		  + "   AND COALESCE(pr_summary.like_count, 0) >= :likeThreshold "
		  + "   AND pr_summary.net_like_count >= :netLikeThreshold "
		  + "   AND p.author_id = :authorId ",
		  nativeQuery = true)
	Page<Post> searchParentBoardPostsByAuthor(@Param("childBoardIds") List<Long> childBoardIds,
											  @Param("authorId") Long authorId,
											  @Param("likeThreshold") int likeThreshold,
											  @Param("netLikeThreshold") int netLikeThreshold,
											  @Param("dayLimit") int dayLimit,
											  Pageable pageable);

	/** 게시글 배치 조회
	    조건 : 수정일자 기준으로 5년 지나고, 조회수가 100이하이며, 
	          공지글이 아니며, ACTIVE상태인 게시글, 'pin'으로 고정된 글 X
	 @return : 삭제돨 게시글 수(레코드) 반환
	*/
	@Query(value = 
			"SELECT p.* "
		  + "  FROM post p "
		  + "  LEFT JOIN comment c ON p.post_id = c.post_id "
		  + " WHERE p.updated_at <= :cutDate "
		  + "   AND p.view_count <= :maxViewCount "
		  + "   AND p.status = 'ACTIVE' "
		  + "   AND p.is_notice = false "
		  + "   AND p.is_pinned = false "
		  + " GROUP BY p.post_id "
		  + "HAVING COUNT(c.comment_id) = 0", nativeQuery = true)
	List<Post> findByDeadPost (@Param("cutDate")LocalDateTime cutDate,
			                   @Param("maxViewCount") int maxViewCount);

	/** 게시글 배치 삭제 
	    조건 : 수정일자 기준으로 5년 지나고, 조회수가 100이하이며, 
	          공지글이 아니며, ACTIVE상태인 게시글, pin으로 고정된 글 X
	 @return : 삭제된 게시글 수(레코드) 반환
	*/
	@Modifying
	@Query(value = 
			"DELETE FROM post "
		  + " WHERE post_id IN ( "
		  + "                   SELECT p.post_id "
		  + "                     FROM post p "
		  + "                     LEFT JOIN comment c ON p.post_id = c.post_id "
		  + "                    WHERE p.updated_at <= :cutDate "
		  + "                      AND p.view_count <= :maxViewCount "
		  + "                      AND p.status = 'ACTIVE' "
		  + "                      AND p.is_notice = false "
		  + "                      AND p.is_pinned = false "
		  + "                    GROUP BY p.post_id "
		  + "                   HAVING COUNT(c.comment_id) = 0 "
		  + "                  )", nativeQuery =  true)
	int deleteDeadPost (@Param("cutDate") LocalDateTime cutDate,
			            @Param("maxViewCount") int maxViewCount);

	/** 공지글 배치 조회
		조건 : 수정일자 기준으로 5년 지나고, 공지글(notice = true), ACTIVE인 게시글
	 @return : 삭제된 공직즐 List 반환
	*/
	@Query(value = 
			"SELECT p.* "
		  + "  FROM post p "
		  + "  LEFT JOIN comment c ON p.post_id = c.post_id "
		  + " WHERE p.updated_at <= :cutDate "
		  + "   AND p.status = 'ACTIVE' "
		  + "   AND p.is_notice = true "
		  + " GROUP BY p.post_id "
		  + "HAVING COUNT(c.comment_id) = 0", nativeQuery = true)
	List<Post> findByDeadNoticePost (@Param("cutDate")LocalDateTime cutDate);

	/** 공지글 배치 삭제 
	    조건 : 수정일자 기준으로 5년 지나고, 공지글(notice = true), ACTIVE인 게시글 
	    @return : 삭제된 게시글 수(레코드) 반환
	*/
	@Modifying
	@Query(value = 
			"DELETE FROM post "
		  + " WHERE post_id IN ( "
		  + "                   SELECT p.post_id "
		  + "                     FROM post p "
		  + "                     LEFT JOIN comment c ON p.post_id = c.post_id "
		  + "                    WHERE p.updated_at <= :cutDate "
		  + "                      AND p.status = 'ACTIVE' "
		  + "                      AND p.is_notice = true "
		  + "                    GROUP BY p.post_id "
		  + "                   HAVING COUNT(c.comment_id) = 0 "
		  + "                  )", nativeQuery =  true)
	int deleteDeadNoticePost (@Param("cutDate") LocalDateTime cutDate);

}
