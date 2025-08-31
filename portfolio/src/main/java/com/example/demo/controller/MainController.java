package com.example.demo.controller;


import java.nio.charset.StandardCharsets;
import java.util.List;

import org.slf4j.Logger;


import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

import com.example.demo.dto.MainPostPageResponseDTO;
import com.example.demo.service.MainService;
import com.example.demo.validation.post.PostValidation;
import com.example.demo.validation.string.WordValidation;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/main")
public class MainController {

	private final MainService mainService;

	private static final Logger logger = LoggerFactory.getLogger(MainController.class);

	// 메인 인기글
	@GetMapping("/popular-posts")
    public ResponseEntity<?> getMainPopularPosts(@PageableDefault(size = 10) Pageable pageable) {

    	logger.info("MainController getMainPopularPosts() Start");
    	
    	MainPostPageResponseDTO response = mainService.getMainPopularPosts(pageable);

        logger.info("MainController getMainPopularPosts() End");        
        return ResponseEntity.ok(response);
    }

	// 메인 인기 게시글 검색
	@GetMapping("/popular_posts/keyword/search")
	public ResponseEntity<?> getMainPopularPostsSearch(@RequestParam(name = "keyword") String keyword,
			                                           @PageableDefault(size = 10) Pageable pageable) {

		logger.info("MainController getMainPopularPostsSearch() Start");

		if(!PostValidation.isValidString(keyword) || keyword.trim().length() < 2) {
			logger.error("MainController getMainPopularPostsSearch() : 'keyword'가 유효하지 않습니다.");
			return ResponseEntity.ok(Page.empty(pageable)); //빈페이지 //빈페이지
		}

		// 한글 깨짐 방지를 위한 UTF_8 인코더
		String keywordUTF8 = UriUtils.decode(keyword, StandardCharsets.UTF_8).trim();

		if(!WordValidation.containsForbiddenWord(keywordUTF8)) {
			logger.error("MainController getMainPopularPostsSearch() : 'keyword'가 유효하지 않습니다.");
			return ResponseEntity.ok(Page.empty(pageable)); //빈페이지
		}

		MainPostPageResponseDTO response = mainService.getMainPopularPostsSearch(keywordUTF8, pageable);

		logger.info("MainController getMainPopularPostsSearch() End");

		return ResponseEntity.ok(response);
	}

	// 메인 인기 작성자 검색
	@GetMapping("/popular_posts/author/search/{nickname}")
	public ResponseEntity<?> getMainPopularPostsAuthor(@PathVariable(name = "nickname") String nickname,
			                                           @PageableDefault(size = 10) Pageable pageable) {

		logger.info("MainController getMainPopularPostsSearch() Start");
		
		if(!PostValidation.isValidString(nickname) || nickname.trim().length() < 2) {
			logger.error("MainController getMainPopularPostsSearch() : 'keyword'가 유효하지 않습니다.");
			return ResponseEntity.ok(Page.empty(pageable)); //빈페이지 //빈페이지
		}

		// 한글 깨짐 방지를 위한 UTF_8 인코더
		String keywordUTF8 = UriUtils.decode(nickname, StandardCharsets.UTF_8).trim();

		if(!WordValidation.containsForbiddenWord(keywordUTF8)) {
			logger.error("MainController getMainPopularPostsSearch() : 'keyword'가 유효하지 않습니다.");
			return ResponseEntity.ok(Page.empty(pageable)); //빈페이지
		}

		MainPostPageResponseDTO response = mainService.getMainPopularPostsAuthor(keywordUTF8, pageable);

		logger.info("MainController getMainPopularPostsSearch() End");

		return ResponseEntity.ok(response);
	}

	// 메인 실시간 검색
	@GetMapping("/popular_posts/autocomplete/search")
	public ResponseEntity<?> getMainPopularPostsAutoComplete(@RequestParam(name = "keyword") String keyword,
			                                                 @PageableDefault(size = 10) Pageable pageable) {

		logger.info("MainController getMainPopularPostsSearch() Start");

		if(!PostValidation.isValidString(keyword) || keyword.trim().length() < 2) {
			logger.error("MainController getMainPopularPostsSearch() : 'keyword'가 유효하지 않습니다.");
			return ResponseEntity.ok(Page.empty(pageable)); //빈페이지 //빈페이지
		}

		// 한글 깨짐 방지를 위한 UTF_8 인코더
		String keywordUTF8 = UriUtils.decode(keyword, StandardCharsets.UTF_8).trim();

		if(!WordValidation.containsForbiddenWord(keywordUTF8)) {
			logger.error("MainController getMainPopularPostsSearch() : 'keyword'가 유효하지 않습니다.");
			return ResponseEntity.ok(Page.empty(pageable)); //빈페이지
		}

		List<String> response = mainService.getMainPopularPostsAutoComplete(keywordUTF8);

		logger.info("MainController getMainPopularPostsSearch() End");

		return ResponseEntity.ok(response);
	}

	// 메인 실시간 검색 조회
	@GetMapping("/popular_posts/autocomplete/title/search")
	public ResponseEntity<?> getMainPopularPostsAutoCompleteSearch(@RequestParam(name = "title") String title,
			                                                       @PageableDefault(size = 10) Pageable pageable) {

		logger.info("MainController getMainPopularPostsSearch() Start");

		if(!PostValidation.isValidString(title) || title.trim().length() < 2) {
			logger.error("MainController getMainPopularPostsSearch() : 'keyword'가 유효하지 않습니다.");
			return ResponseEntity.ok(Page.empty(pageable)); //빈페이지 //빈페이지
		}

		// 한글 깨짐 방지를 위한 UTF_8 인코더
		String keywordUTF8 = UriUtils.decode(title, StandardCharsets.UTF_8).trim();

		if(!WordValidation.containsForbiddenWord(keywordUTF8)) {
			logger.error("MainController getMainPopularPostsSearch() : 'keyword'가 유효하지 않습니다.");
			return ResponseEntity.ok(Page.empty(pageable)); //빈페이지
		}

		MainPostPageResponseDTO response = mainService.getMainPopularPostsAutoCompleteSearch(keywordUTF8,pageable);

		logger.info("MainController getMainPopularPostsSearch() End");

		return ResponseEntity.ok(response);
	}

}
