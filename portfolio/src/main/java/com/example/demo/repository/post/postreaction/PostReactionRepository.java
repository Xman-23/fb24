package com.example.demo.repository.post.postreaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.post.Post;
import com.example.demo.domain.post.postreaction.PostReaction;
import com.example.demo.domain.post.postreaction.postreactionenums.PostReactionType;

import java.util.*;

@Repository
public interface PostReactionRepository extends JpaRepository<PostReaction, Long> {

	// 특정 게시글과 사용자 ID로 반응 조회 (좋아요/싫어요 중복 방지용)
	Optional<PostReaction> findByPostAndUserId(Post post, Long memberId);

    // 게시글 수정에 사용하는 각 '좋아요', '싫어요' 갯수 조회
    int countByPostPostIdAndReactionType(Long postId, PostReactionType reactionType);

    // 배치에 사용될 삭제 쿼리
    @Modifying // 'UPDATE'or'DELETE'
    @Query(
    		"DELETE FROM PostReaction pr "
    	  + " WHERE pr.userId IN (:userIds) "
    	  )
    void deleteAllByUserIdIn(@Param("userIds")List<Long> userIds);

	/** Query 결과
	* postId    | likeCount | dislikeCount
	* 101       | 10        | 2     // 댓글 101에 대한 좋아요 2개, 싫어요 2개
	* 102       | 3         | 1     // 댓글 102에 대한 좋아요 10개, 싫어요 1개 
	* 103       | 4         | 1     // 댓글 103에 대한 좋아요 4개, 싫어요 1개
	* 104       | 3         | 1     // 댓글 104에 대한 좋아요 3개, 싫어요 1개
	* 105       | 5         | 1     // 댓글 105에 대한 좋아요 5개, 싫어요 1개
	*/
 
    // 메인화면에 사용할 좋아요 집계조회
    @Query(
    		"SELECT pr.post.postId AS postId, "
    	  + "       SUM(CASE "
    	  + "               WHEN pr.reactionType = 'LIKE' THEN 1 ELSE 0 "
    	  + "            END) AS likeCount "
    	  + "  FROM PostReaction pr "
    	  + " WHERE pr.post.postId IN (:postIds) "
    	  + " GROUP BY pr.post.postId "
    	  ) 
    List<PostLikeReactionCount> countLikeReactionsByPostIds(@Param("postIds") List<Long> postIds);

    interface PostLikeReactionCount {
        Long getPostId();
        Long getLikeCount();
    }

    // 상세 게시글에 사용할 좋아요, 싫어요 집계조회
    @Query(
    	    "SELECT p.postId AS postId, "
		  + "       COALESCE(SUM(CASE WHEN pr.reactionType = 'LIKE' THEN 1 ELSE 0 END), 0) AS likeCount, "
		  + "       COALESCE(SUM(CASE WHEN pr.reactionType = 'DISLIKE' THEN 1 ELSE 0 END), 0) AS dislikeCount "
		  + "  FROM Post p "
		  + "  LEFT JOIN p.reactions pr "
		  + " WHERE p.postId = :postId "
		  + " GROUP BY p.postId"
    	  ) 
    PostReactionCount countReactionsByPostId(@Param("postId") Long postId);

    public interface PostReactionCount {
        Long getPostId();
        Long getLikeCount();
        Long getDislikeCount();
    }

    // 게시글 삭제시 리액션 모두 삭제
    @Modifying // 'UPDATE' or 'DELETE'
    @Query(
    		"DELETE FROM PostReaction pr "
    	  + " WHERE pr.post = :post "
    	  )
    int deleteByPost(@Param("post")Post post);

}
