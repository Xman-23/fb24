package com.example.demo.service.post.postreaction;

import java.util.List;

import com.example.demo.dto.post.postreaction.PostReactionRequestDTO;
import com.example.demo.dto.post.postreaction.PostReactionResponseDTO;

public interface PostReactionService {

	// 게시글 리액션 
	PostReactionResponseDTO reactionToPost(Long postId, 
			                               Long memberId,  
			                               PostReactionRequestDTO postReactionRequestDTO );
	

	// 배치로 게시글 휴먼 계정 반응 삭제 ( 0 0 3 * * ? => 매일 새벽 3시)
	void postRemoveReactionsByDeadtUsers (List<Long> userIds);

}
