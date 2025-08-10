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

    // 각 '좋아요', '싫어요' 갯수 조회
    int countByPostPostIdAndReactionType(Long postId, PostReactionType reactionType);

    @Modifying // 'UPDATE'or'DELETE'
    @Query(
    		"DELETE FROM PostReaction pr "
    	  + " WHERE pr.userId IN (:userIds) "
    	  )
    void deleteAllByUserIdIn(@Param("userIds")List<Long> userIds);

}
