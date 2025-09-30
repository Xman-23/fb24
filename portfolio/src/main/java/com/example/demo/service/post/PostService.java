package com.example.demo.service.post;


import com.example.demo.dto.post.PostNoticeBoardResponseDTO;




import com.example.demo.dto.post.PostParentBoardPostPageResponseDTO;
import com.example.demo.dto.post.PostBoardPostSearchPageResponseDTO;
import com.example.demo.dto.MainPostPageResponseDTO;
import com.example.demo.dto.post.PostCreateRequestDTO;

import com.example.demo.dto.post.PostPageResponseDTO;
import com.example.demo.dto.post.PostResponseDTO;
import com.example.demo.dto.post.PostUpdateRequestDTO;
import com.example.demo.dto.post.postimage.ImageOrderDTO;
import com.example.demo.dto.post.postimage.PostImageResponseDTO;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
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
	void deletePost(Long postId, Long authorId);

	// 게시글 신고
	String reportPost(Long postId, Long reporterId, String reason);

	// 전체 공지글 (공지 게시판 전용)
	PostNoticeBoardResponseDTO getAllNotices(Pageable pageable);

	// 전체 공지글 키워드 검색
	PostBoardPostSearchPageResponseDTO noticeBoardSearchPosts (String keyword, Pageable pageable);

	// 전체 공지글 키워드 자동완성
	List<String> noticePostTitlesByKeyword(String keyword);

	PostBoardPostSearchPageResponseDTO autocompleteSearchPostsByNoticeBoard(String title,Pageable pageable);

	// 전체 공지글 닉네임 검색
	PostBoardPostSearchPageResponseDTO noticeBoardSearchPostsAndAuthor (String nickname, Pageable pageable);

	// 자식 게시판의 게시글 목록 조회 (ACTIVE + 공지글 제외)
	PostPageResponseDTO getPostsByBoard(Long boardId, Pageable pageable);

	// 자식 게시판 정렬
	PostPageResponseDTO getPostsByBoardSorted(Long boardId, String sortBy, Pageable pageable);

	// 자식 게시판 키워드 검색
	PostBoardPostSearchPageResponseDTO childBoardSearchPosts(Long boardId, String keyword, Pageable pageable);

	// 자식 게시팜 닉네임 검색
	PostBoardPostSearchPageResponseDTO childBoardSearchPostsAndAuthor(Long boardId, String nickname, Pageable pageable);

	// 자식 게시판 키워드 자동완성
	List<String> childPostTitlesByKeyword(Long boardId, String keyword);

	PostBoardPostSearchPageResponseDTO childPostSearchTitlesByKeyword(Long boardId, String title, Pageable pageable);

	// 통합 게시글 키워드 검색 (제목 또는 본문에 키워드 포함 + ACTIVE 상태)
	MainPostPageResponseDTO searchPostsByKeyword(String keyword, Pageable pageable);

	// 통합 작성자별 게시글 조회
	MainPostPageResponseDTO getPostsByAuthorNickname(String nickname,Pageable pageable);

	// 통합 실시간 검색 제목 게시글 조회
	MainPostPageResponseDTO getSearchPostsByAuthorNickname(String title, Pageable pageable);

	// 조회수 중복 방지
	void increaseViewCount(Long postId, Long memberId, String ip);

	// 핀 게시글 설정/해제
	void togglePinPost(Long postId, boolean pin);

	// 이미지 생성
	void savePostImages(Long postId, List<MultipartFile> files);

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

	// 부모게시판 페이징
	PostParentBoardPostPageResponseDTO getPostsByParentBoard (Long parentBoardId, Pageable pageable);
	
	// 부모 게시판 키워드 검색
	PostBoardPostSearchPageResponseDTO searchPostsByParentBoard(Long parentBoardId, String keyword, Pageable pageable);
	// 부모게시판 실시간 검색
	List<String> postPostTitlesByKeyword(Long parentBoardId, String keyword);
	
	PostBoardPostSearchPageResponseDTO autocompleteSearchPostsByParentBoard(Long parentBoardId, String title, Pageable pageable);
	// 부모 게시판 닉네임 검색
	PostBoardPostSearchPageResponseDTO searchPostsByParentBoardAndAuthor(Long parentBoardId, String nickname, Pageable pageable);
	

	// 게시글 배치 삭제
	int deleteDeadPost(LocalDateTime cutDate, int maxViewCount);

	// 공지 게시글 배치 삭제
	int deleteDeadNoticePost(LocalDateTime cutDate);
	
	// 검색어 자동완성
	List<String> getPostTitlesByKeyword(String keyword);

	// 내정보 게시글 보기용
	PostPageResponseDTO getPostsByAuthor(Long memberId, Pageable pageable);

}
