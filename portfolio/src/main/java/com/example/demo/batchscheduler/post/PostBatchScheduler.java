package com.example.demo.batchscheduler.post;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.demo.domain.post.Post;
import com.example.demo.repository.post.PostRepository;
import com.example.demo.service.post.PostService;

import lombok.RequiredArgsConstructor;

/**
 * @Component로 선언된 클래스는 스프링이 빈(객체)으로 등록하여,
 * 스프링 컨테이너가 객체의 생명주기를 자동으로 관리한다.
 * 스프링 컨테이너는 관리하는 빈(객체)을 모아놓은 바구니 으로,
 * 필요할 때 새로 생성하지 않고 기존에 생성된 객체를 꺼내서 사용한다.
 * 따라서 배치 클래스도 @Component로 선언되어야 객체가 생성되고,
 * @Scheduled(cron)으로 설정한 메소드가 자동으로 실행된다
 */
@Component
@RequiredArgsConstructor
public class PostBatchScheduler {

	// 서비스
	private final PostService postService;

	// 레파지토리
	private final PostRepository postRepository;

	// 배치로 삭제할 연도 기준
	private final long CUT_YEARS = 5L;
	// 배치로 삭제할 조회수 기준
	private final int CUT_VIEW_COUNT = 100;

	// 로그
	private static final Logger logger = LoggerFactory.getLogger(PostBatchScheduler.class);

	// 매월 1일 자정(0시0분0초)에 실행
	// 초(0~59), 분(0~59), 시간(0~23)
	// 일(1~31, 월(1~12) , (0,7(일요일), 1~6(월~토)
	@Scheduled(cron = "0 0 0 1 * *")
	public void runDeadPostBatchDelete() {

		logger.info("PostBatchScheduler runDeadPostBatchDelete() Start");

		// 현재 시간을 기준으로 5년을뺀 날짜를 기준
		LocalDateTime cutDate = LocalDateTime.now().minusYears(CUT_YEARS);
		// 삭제될 게시글 조회
		List<Post> deadPosts = postRepository.findByDeadPost(cutDate, CUT_VIEW_COUNT);
		if(deadPosts == null || deadPosts.isEmpty()) {
			logger.info("PostBatchScheduler runDeadPostBatchDelete() 삭제할 대상이 없으므로 조기 종료");
			return;
		}
		logger.info("PostBatchScheduler runDeadPostBatchDelete() 삭제될 게시글 : {}", deadPosts);
		int deadPostsCount = postService.deleteDeadPost(cutDate, CUT_VIEW_COUNT);
		logger.info("PostBatchScheduler runDeadPostBatchDelete() 삭제된 게시글 개수 : {}", deadPostsCount);

		logger.info("PostBatchScheduler runDeadPostBatchDelete() End");
	}

	@Scheduled(cron = "0 0 6 1 * *")
	public void runDeadNoticePostBatchDelete() {

		logger.info("PostBatchScheduler runDeadNoticePostBatchDelete() Start");

		// 현재 시간을 기준으로 5년을뺀 날짜를 기준
		LocalDateTime cutDate = LocalDateTime.now().minusYears(CUT_YEARS);

		// 삭제될 공지글 조회
		List<Post> deadNoticePosts = postRepository.findByDeadNoticePost(cutDate);
		if(deadNoticePosts == null || deadNoticePosts.isEmpty()) {
			logger.info("PostBatchScheduler runDeadNoticePostBatchDelete() 삭제할 대상이 없으므로 조기 종료");
			return;
		}
		logger.info("PostBatchScheduler runDeadNoticePostBatchDelete() 삭제될 공지글 : {}", deadNoticePosts);
		int deadNoticePostsCount = postService.deleteDeadNoticePost(cutDate);
		logger.info("PostBatchScheduler runDeadNoticePostBatchDelete() 삭제된 공지 게시글 개수 : {}", deadNoticePostsCount);

		logger.info("PostBatchScheduler runDeadNoticePostBatchDelete() End");
	}
}
