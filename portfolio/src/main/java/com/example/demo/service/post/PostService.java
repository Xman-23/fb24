package com.example.demo.service.post;

import com.example.demo.dto.post.PostCreateRequestDTO;

import com.example.demo.dto.post.PostListResponseDTO;
import com.example.demo.dto.post.PostResponseDTO;
import com.example.demo.dto.post.PostUpdateRequestDTO;
import com.example.demo.dto.postimage.ImageOrderDTO;
import com.example.demo.dto.postimage.PostImageResponseDTO;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;


public interface PostService {

	// 게시글 생성 Service
	PostResponseDTO createPost(PostCreateRequestDTO postCreateRequestDTO,
										  Long authorId);

	// 게시글 단건 조회
	PostResponseDTO getPost(Long postId);

	// 게시글 수정
	PostResponseDTO updatePost(Long postId, 
							   PostUpdateRequestDTO postUpdateRequestDTO,
							   Long authorId);

	// 게시글 삭제
	void deletePost(Long postId, Long authorId);

	// 전체 공지글 (공지 게시판 전용)
	Page<PostListResponseDTO> getAllNotices(Pageable pageable);

	// 특정 게시판의 게시글 목록 조회 (ACTIVE + 공지글 제외)
	Page<PostListResponseDTO> getPostsByBoard(Long boardId, Pageable pageable);

	// 게시글 키워드 검색 (제목 또는 본문에 키워드 포함 + ACTIVE 상태)
	Page<PostListResponseDTO> searchPostsByKeyword(String keyword, Pageable pageable);

	// 인기순 정렬 (좋아요 + 댓글 수를 기준으로 내림차순으로 정렬한 후 생성일자로 다시한번 내림차순)
	Page<PostListResponseDTO> getPostsSorted(String sortBy, Pageable pageable);

	// 작성자별 게시글 조회
	Page<PostListResponseDTO> getPostsByAuthorNickname(String nickname,Pageable pageable);

	// 조회수 중복 방지
	void increaseViewCount(Long postId, String userIdentifier);

	// 핀 게시글 설정/해제
	void togglePinPost(Long postId, boolean pin);

	// 3개의 핀으로 설정된 공지 게시글 모든 게시판에 보여주기
	List<PostListResponseDTO> getTop3PinnedNoticesByBoard();

	// 이미지 생성
	List<String> savePostImages(Long postId, List<MultipartFile> files);

	// 이미지 삭제
	void deletePostImages(Long postId);

	// 이미지 목록 조회
	List<PostImageResponseDTO> getPostImages(Long postId);

	// 이미지 정렬 순서 조정
	void updateImageOrder(Long postId, List<ImageOrderDTO> orderList, Long requestAuthorId);

	// 이미지 단건 삭제
	void deleteSingleImage(Long postId, Long imageId, Long requestAuthorId);

	//
	public Page<PostListResponseDTO> getPostsByParentBoard (Long parentBoardId, Pageable pageable);


}
