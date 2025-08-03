package com.example.demo.service.post;

import com.example.demo.domain.board.Board;
import com.example.demo.domain.post.Post;
import com.example.demo.dto.post.PostCreateRequestDTO;

import com.example.demo.dto.post.PostListResponseDTO;
import com.example.demo.dto.post.PostResponseDTO;
import com.example.demo.dto.post.PostUpdateRequestDTO;
import com.example.demo.dto.postimage.ImageOrderDTO;
import com.example.demo.dto.postimage.PostImageResponseDTO;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.web.multipart.MultipartFile;


public interface PostService {

	// 게시글 생성 Service
	PostResponseDTO createPost(PostCreateRequestDTO postCreateRequestDTO,
							   Long authorId,
							   String userNickname);

	// 게시글 단건 조회
	PostResponseDTO getPost(Long postId);

	// 게시글 수정
	PostResponseDTO updatePost(Long postId, 
							   PostUpdateRequestDTO postUpdateRequestDTO,
							   Long authorId,
							   String userNickname);

	// 게시글 삭제
	void deletePost(Long postId, Long authorId, boolean isDeleteImages);

	// 전체 공지글 (공지 게시판 전용)
	Page<PostListResponseDTO> getAllNotices(Pageable pageable);

	// 특정 게시판의 게시글 목록 조회 (ACTIVE + 공지글 제외)
	Page<PostListResponseDTO> getPostsByBoard(Long boardId, Pageable pageable);

	// 게시글 키워드 검색 (제목 또는 본문에 키워드 포함 + ACTIVE 상태)
	Page<PostListResponseDTO> searchPostsByKeyword(String keyword, Pageable pageable);

	// 자식 게시판 정렬
	Page<PostListResponseDTO> getPostsByBoardSorted(Long boardId, String sortBy, Pageable pageable);

	// 부모 게시판 정렬
	Page<PostListResponseDTO> getPostsByParentBoard(Long parentBoardId, String sortBy, Pageable pageable);

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
	List<PostImageResponseDTO> updateImageOrder(Long postId, List<ImageOrderDTO> orderList, Long requestAuthorId);

	// 이미지 단건 삭제
	void deleteSingleImage(Long postId, Long imageId, Long requestAuthorId);

	// 이미지 모두 삭제
	void deleteAllImages(Long postId, Long requestAuthorId);

	Page<PostListResponseDTO> getPostsByParentBoard (Long parentBoardId, Pageable pageable);


}
