package com.example.demo.batchscheduler.post.postreaction;

import java.util.List;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.demo.domain.member.Member;
import com.example.demo.domain.member.memberenums.MemberStatus;
import com.example.demo.repository.member.MemberRepository;
import com.example.demo.service.post.postreaction.PostReactionService;

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
public class PostReactionBatchScheduler {

	// 서비스
	private final PostReactionService postReactionService;

	// 레파지토리
	private final MemberRepository memberRepository;

	// 멤버 상태 (Active 제외한 모든 상태)
	private final List<MemberStatus> MEMBER_STATUSES = List.of(MemberStatus.DORMANT,
			                                                  MemberStatus.SUSPENDED,
			                                                  MemberStatus.WITHDRAWN);

	// 로그
	private static final Logger logger = LoggerFactory.getLogger(PostReactionBatchScheduler.class);

	//*************************************************** BatchScheduler START ***************************************************//

	// 초(0~59), 분(0~59), 시간(0~23)
	// 일(1~31, 월(1~12) , (0,7(일요일), 1~6(월~토)
	// 요일 중 일요일에 '일' 상관없이 0시0분0초에 배치 
	@Scheduled(cron = "0 0 0 ? * 0")
	public void removeReactionsOfDeadUsers() {

		logger.info("ReactionBatchScheduler removeReactionsOfDeadUsers() Start");

		// 휴먼 상태인 회원 리스트 조회
		List<Member> deadMember = memberRepository.findByStatuses(MEMBER_STATUSES);

		if(deadMember.isEmpty()) {
			logger.info("CommentReactionBatchScheduler removeReactionsOfDeadtUsers() : 삭제할 대상이 없어 메서드 조기종료");
			return;
		}

		logger.info("ReactionBatchScheduler removeReactionsOfDeadUsers() 휴먼 상태인 회원 : {}", deadMember);

		// 휴먼 회원의 userId 추출
		List<Long> deadMemberId = deadMember.stream()
				                            .map(member -> member.getId())
				                            .collect(Collectors.toList());

		postReactionService.postRemoveReactionsByDeadtUsers(deadMemberId);

		logger.info("ReactionBatchScheduler removeReactionsOfDeadUsers() End");
	}

	//*************************************************** BatchScheduler END ***************************************************//

}
