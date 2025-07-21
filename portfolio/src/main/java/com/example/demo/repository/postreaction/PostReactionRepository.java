package com.example.demo.repository.postreaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.post.Post;
import com.example.demo.domain.postreaction.PostReaction;
import com.example.demo.domain.postreaction.enums.ReactionType;

import java.util.*;

@Repository
public interface PostReactionRepository extends JpaRepository<PostReaction, Long> {

	// 특정 게시글에 대한 모든 반응 조회 (좋아요, 싫어요) 조회
	List<PostReaction> findByPost(Post post);

	// 특정 게시글과 사용자 ID로 반응 조회 (좋아요/싫어요 중복 방지용)
	Optional<PostReaction> findByPostAndUserId(Post post, Long userId);

	// 특정 사용자 ID로 모든 반응 조회
	List<PostReaction> findByUserId(Long userId);

	// 특정 게시글에 좋아요 수 조회 (메인페이지에 좋아요 많은 게시글 띄우는데 필요한 메서드)
	long countByPostAndReactionType(Post post, ReactionType reactionType);

    // 특정 게시글에 특정 사용자 반응 삭제 (좋아요, 싫어요 눌렀던 게시글에서 반응 삭제)
    void deleteByPostAndUserId(Post post, Long userId);
   
    // 특정 게시글에 특정 사용자 반응 존재 여부 확인 (if문으로 유효성 체크할때 사용)
    boolean existsByPostAndUserId(Post post, Long userId);
}
