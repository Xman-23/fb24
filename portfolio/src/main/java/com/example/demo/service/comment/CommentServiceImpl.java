package com.example.demo.service.comment;

import java.nio.charset.StandardCharsets;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;

import com.example.demo.domain.comment.Comment;
import com.example.demo.domain.comment.commentenums.CommentStatus;
import com.example.demo.domain.comment.commentreport.CommentReport;
import com.example.demo.domain.member.Member;
import com.example.demo.domain.post.Post;
import com.example.demo.domain.post.postreaction.postreactionenums.PostReactionType;
import com.example.demo.dto.comment.CommentPageResponseDTO;
import com.example.demo.dto.comment.CommentRequestDTO;
import com.example.demo.dto.comment.CommentResponseDTO;
import com.example.demo.dto.comment.commentreport.CommentReportResponseDTO;
import com.example.demo.repository.comment.CommentRepository;
import com.example.demo.repository.comment.commentreaction.CommentReactionRepository;
import com.example.demo.repository.comment.commentreport.CommentReportRepository;
import com.example.demo.repository.member.MemberRepository;
import com.example.demo.repository.post.PostRepository;
import com.example.demo.service.notification.NotificationService;
import com.example.demo.validation.string.WordValidation;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

	private final MemberRepository memberRepository;
    private final CommentRepository commentRepository;
    private final CommentReportRepository commentReportRepository;
    private final PostRepository postRepository;
    private final CommentReactionRepository commentReactionRepository;

    private final NotificationService notificationServiceImpl;

    // 좋아요 기준
    @Value("${comment.topPinned.likeThreshold}")
    private int likeThreshold;

    // 댓글 상위 탑 3개
    @Value("${comment.topPinned.limit}")
    private int topLimit;
 
    // 댓글 좋아요 싫어요 차이 기준
    @Value("${comment.topPinned.netLikeThreshold}")
    private int netLikeThreshold;

    // 댓글 신고 제한
    @Value("${comment.report.threshold}")
    private int reportThreshold;

    private static final Logger logger = LoggerFactory.getLogger(CommentServiceImpl.class);


    // DFS(재귀함수) : 자식 댓글 최신순 정렬
    private void sortChildComments(List<CommentResponseDTO> comments, boolean isRoot) {

    	// 대댓글이 없는 부모댓글 또는 상단 3개 댓글일 경우 'return'후 스택에서 메서드 종료
    	if(comments == null || comments.isEmpty()) {
    		// '재귀 종료'를 알리는 'return'
    		return;
    	}

    	// 처음에 'true'로 던져 줬기때문에 부모댓글 또는 상단 3개는 최신순으로 정렬하지 않고,
    	// 대댓글만 최신순으로 정렬, 부모와 상단 댓글은 순서 변경이 되지 않음
    	if(!isRoot) {
    		// 하지만!! 만약, '대댓글' 중에서 '상단에 고정된 대댓글'이 있을 경우, 그 '대댓글은' 최신순 정렬에서 제외 시켜줘야함
    		// => 부모 댓글 → (고정 대댓글) → (고정 아닌 대댓글들 최신순)

    		// pinned(고정) 대댓글 분리
    		List<CommentResponseDTO> pinnedComments = comments.stream()
    				                                          .filter(comment -> comment.isPinned())
    				                                          .collect(Collectors.toList());

    		// NotPinned(고정되지않은) 대댓글 분리
    		List<CommentResponseDTO> notPinnedComments = comments.stream()
    				                                             .filter(comment -> !comment.isPinned())
    				                                             .collect(Collectors.toList());

    		// 고정되지 않은 대댓글 최신순 정렬 ('b'댓글 생성기준으로 비교하면 내림차순)
    		notPinnedComments.sort((a,b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

    		// '자식 댓글'들의 정렬 순서를 초기화 (대댓글, 대댓글, 대댓글(고정))
    		comments.clear();
    		// '자식 댓글'중에 '고정된'댓글을 먼저 추가해 보여주고 (대댓글(고정)
    		comments.addAll(pinnedComments); 
    		// 나머지 '자식 댓글'을 추가해서 보여줌 (대댓글(고정, 대댓글, 대댓글)
    		comments.addAll(notPinnedComments);
    	}

    	for(CommentResponseDTO comment : comments) {
    		// 정렬이 된 'List'안에 '부모 댓글'의 'getChildComments()'를 사용하여 '자식댓글(대댓글)'들을 꺼내서 던짐
    		sortChildComments(comment.getChildComments(),false);
    	}
    }

    private CommentResponseDTO convertCommentToDtoRecursive(Comment comment, Map<Long, Map<PostReactionType, Long>> reactionCountMap) {

    	// 리액션(좋아요, 싫어요) 꺼내기
    	Map<PostReactionType, Long> counts = reactionCountMap.getOrDefault(comment.getCommentId(), Collections.emptyMap());

    	// 좋아요 갯수 가져오기 없으면 '0'반환
    	int likeCount = counts.getOrDefault(PostReactionType.LIKE, 0L).intValue();
    	// 싫어요 갯수 가져오기 없으면 '0'반환
        int dislikeCount = counts.getOrDefault(PostReactionType.DISLIKE, 0L).intValue();

        // 댓글 DTO로 반환
        CommentResponseDTO dto = CommentResponseDTO.fromEntity(comment, likeCount, dislikeCount, false, comment.getMember().getNickname());

        // 자식댓글이 존재한다면
        if (comment.getChildComments() != null && !comment.getChildComments().isEmpty()) {
        	// Comment -> List<Comments(자식댓글)> -> Stream<Comments(자식댓글)>
            List<CommentResponseDTO> childDtos = comment.getChildComments().stream()
            											// 여기서 재귀 시작, 자식 댓글 있으면 계속 재귀
            											// 1번 부모 댓글이 끝나면, 2번 부모 댓글 재귀..... 모든 부모댓글 재귀 돌기
                                                        .map(child -> convertCommentToDtoRecursive(child, reactionCountMap))
                                                        // CommentResponseDTO 변환 후 'List'로 변환
                                                        .collect(Collectors.toList());
            dto.setChildComments(childDtos);
        } else {
        	// 부모댓글은 항상 자식 댓글이 존재할 수 없으므로,
        	// 자식 댓글이 존재 하지않는다면 빈 ArrayList<>() 생성 
        	// -> 'null' 추가할시 NullPointException 방지를 위해서 생성
            dto.setChildComments(new ArrayList<>());
        }
        // 'else'분기를 거치면 자식이 없다는것이므로,
        // 자식필드에 ArrayList<>() 생성후, 재귀 종료
        return dto;
    }

    //*************************************************** Service START ***************************************************//

    // 댓글 생성 Service
	@Override
	public CommentResponseDTO createComment(CommentRequestDTO commentRequestDTO, Long authorId) {

		logger.info("CommentServiceImpl createComment() Start");

		logger.info("CommentServiceImpl createComment () authorId : {}" , authorId);
		// Request
		Long requestPostId = commentRequestDTO.getPostId();
		String requestContent = UriUtils.decode(commentRequestDTO.getContent(), StandardCharsets.UTF_8);
		Long requestParentCommentId = commentRequestDTO.getParentCommentId();

		Member member = memberRepository.findById(authorId)
                                        .orElseThrow(() -> {
                                        	logger.error("CommentServiceImpl createComment() NoSuchElementException : 회원이 존재하지 않습니다.");
                                        	return new NoSuchElementException("회원이 존재하지 않습니다.");
                                        });
		
		String authorNickname = member.getNickname();

		if(!WordValidation.containsForbiddenWord(requestContent)) {
			logger.error("CommentServiceImpl createComment() IllegalArgumentException : 댓글 내용에 비속어가 포함되어있습니다.");
			throw new IllegalArgumentException("댓글 내용에 비속어가 포함되어있습니다.");
		}

		// 게시글 유효검사
		Post post = postRepository.findById(requestPostId)
		                          .orElseThrow(() -> {
		                        	logger.error("CommentServiceImpl createComment() NoSuchElementException Error : {} ");
		                        	return new NoSuchElementException("게시글이 존재하지 않습니다.");
		                          });

		// 댓글 or 대댓글 유효성 검사를 위한 변수 (Null = 댓글, NotNull = 대댓글)  
		Comment parentComment = null;

		// 대댓글 유효 검사
		// requestParentCommentId == null 이면 부모 댓글 , != null 이면 대댓글
		if(requestParentCommentId != null) {
			parentComment = commentRepository.findById(requestParentCommentId)
					                         .orElseThrow(() -> {
					                        	 logger.error("CommentServiceImpl createComment() NoSuchElementException Error : 부모 댓글이 존재하지 않습니다.");
					                        	 return new NoSuchElementException("부모 댓글이 존재하지 않습니다.");
					                         });

		}
		
		// @Bulider가 붙은 클래스는 객체 생성시 초기값을 무시할 수 있으므로, 직접 값을 넣어주는게 안전하다
		Comment comment = Comment.builder()
				                 .post(post)
				                 .parentComment(parentComment)
				                 .member(member)
				                 .content(requestContent)
				                 .reportCount(0L)
				                 .status(CommentStatus.ACTIVE)
				                 .isPinned(false)
				                 .authorNickname(authorNickname)
				                 .build();

		// 댓글이 'INSERT' 되기전에 등급을 올림
		// 만약 댓글은 올라갔지만, 중간에 트랜잭션이 종료된다면,
		// '@Transactional'어노테이션에 의해서 등급 점수가 롤백되므로 안전함. 
		member.addCommentScore(); // 등급 점수 올리기

		// 'INSERT' 후 'Comment' Entity 반환
		Comment saved = commentRepository.save(comment);

		// 알림을 받을 대상자 (게시글 작성자(자신제외), 부모댓글 작성자)

		if(saved.getParentComment() == null) {
			// 만약 부모 댓글(일반 댓글)이라면, 게시글 작성자에게 알림 보내야됨
			// 게시글 작성자 ID
			Long receiverPostId = saved.getPost().getAuthor().getId();

			// 게시글 작성자가 작성한 댓글은 알림 제외 
			if(!receiverPostId.equals(authorId)) {
				// 댓글 작성자와 게시글 작성자가 다를시 알림 전송
				notificationServiceImpl.notifyPostComment(saved);
			}
		}else {
			// 만약 대댓글(parentComment!=null)이라면, 부모 댓글 작성자에게 알림 보내기
			// 부모댓글(댓글)작성자 ID
			Long receiverCommentId = saved.getParentComment().getMember().getId();
			// 부모댓글(댓글)작성자와 대댓글 작성자가 다를 경우에만 '대댓글 알림' 전송
			if(!receiverCommentId.equals(authorId)) {
				notificationServiceImpl.notifyChildComment(comment);
			}
			// 그리고 대댓글도 게시글의 댓글이므로, 
			// 게시글 작성자에게 알림을 보내야하므로 게시글 작성자 ID 가져옴
			Long receiverPostId = saved.getPost().getAuthor().getId();
			// 게시글 작성자와 대댓글 작성자가 다를 경우에만 '게시글 댓글' 알림 전송
			if(!receiverPostId.equals(authorId)) {
				notificationServiceImpl.notifyPostComment(comment);
			}
		}
		logger.info("CommentServiceImpl createComment() End");
		return CommentResponseDTO.fromEntity(saved, 0, 0, false, authorNickname);
	}

	// 댓글 수정 Service
	@Override
	public CommentResponseDTO updateComment(Long commentId, String content, Long authorId) {

		logger.info("CommentServiceImpl updateComment() Start");

		Comment comment = commentRepository.findById(commentId)
		                                   .orElseThrow(() -> {
		                                	   logger.error("CommentServiceImpl updateComment() NoSuchElementException Error : {}");
		                                	   return new NoSuchElementException("댓글이 존재하지 않습니다.");
		                                   } );


		Member member = memberRepository.findById(authorId)
                						.orElseThrow(() -> {
                										logger.error("CommentServiceImpl createComment() NoSuchElementException : 회원이 존재하지 않습니다.");
                										return new NoSuchElementException("회원이 존재하지 않습니다.");
                									});

		String authorNickname = member.getNickname();

		// DB 변경여부
		boolean dbSetting = false;

		// DB
		String dbComment = comment.getContent();

		if(!WordValidation.containsForbiddenWord(content)) {
			logger.error("CommentServiceImpl updateComment() IllegalArgumentException : 댓글 내용에 비속어가 포함되어있습니다.");
			throw new IllegalArgumentException("댓글 내용에 비속어가 포함되어있습니다.");
		}

		if(!comment.getMember().getId().equals(authorId)) {
			logger.error("CommentServiceImpl updateComment() SecurityException Error : 본인의 댓글만 수정할 수 있습니다.");
			throw new SecurityException("본인의 댓글만 수정할 수 있습니다.");
		}



		// 수정 요청 댓글과 기존의 DB댓글과 다를겨웅에만 업데이트
		if(!content.equals(dbComment)) {
			comment.setContent(content);
			dbSetting = true;
		}

		// 만약 DB에 한번이라도 업데이트가 이뤄지지 않았다면,
		if(dbSetting == false) {
			// 예외 처리로 조기 종료
			logger.error("CommentServiceImpl updateComment() IllegalArgumentException : 잘못된 접근 입니다.");
			throw new IllegalArgumentException("잘못된 접근 입니다.");
		}

		int likeCount = commentReactionRepository.countByCommentAndReactionType(comment, PostReactionType.LIKE);
		int dislikeCount = commentReactionRepository.countByCommentAndReactionType(comment, PostReactionType.DISLIKE);

		logger.info("CommentServiceImpl updateComment() End");
		return CommentResponseDTO.fromEntity(comment, likeCount, dislikeCount, false, authorNickname);
	}

	// 댓글 삭제 Service
	@Override
	public CommentResponseDTO deleteComment(Long commentId, Long authorId) {

		logger.info("CommentServiceImpl deleteComment() Start");

		Comment comment = commentRepository.findById(commentId)
				                           .orElseThrow(() -> {
				                        	   logger.error("CommentServiceImpl deleteComment() NoSuchElementException commentId : {} ", commentId);
				                        	   return new NoSuchElementException("댓글이 존재하지 않습니다.");
				                           });

		Member member = memberRepository.findById(comment.getMember().getId())
				                        .orElseThrow(() -> {
				                        	logger.error("CommentServiceImpl deleteComment() NoSuchElementException memberId: {} ",comment.getMember().getId());
				                        	return new NoSuchElementException("회원이 존재하지 않습니다.");
				                        });

		if(!comment.getMember().getId().equals(authorId)) {
			logger.error("CommentServiceImpl deleteComment() SecurityException Error: 본인의 댓글만 삭제할 수 있습니다.");
			throw new SecurityException("본인의 댓글만 삭제할 수 있습니다.");
		}


		// 해당 댓글 리액션 모두 삭제
		commentReactionRepository.deleteByComment(comment);

		comment.setStatus(CommentStatus.DELETED);
		comment.setAuthorNickname("");

		// 해당 댓글 삭제시 점수 '-1'점 차감
		member.deleteCommentScore();

		logger.info("CommentServiceImpl deleteComment() End");
		return CommentResponseDTO.fromEntity(comment, 0, 0, false, "");
	}

	// 댓글 신고 Service
	@Override
	public CommentReportResponseDTO reportComment(Long commentId, Long reporterId, String reason) {

		logger.info("CommentServiceImpl reportComment() Start");

		// 신고당한 댓글 가져오기 Entity
		// 'JPA'의해 영속성 상태
		Comment comment = commentRepository.findById(commentId)
				                           .orElseThrow(() -> {
				                        	   logger.error("CommentServiceImpl reportComment() NoSuchElementException : 댓글이 존재하지 않습니다.");
				                        	   return new NoSuchElementException("댓글이 존재하지 않습니다.");
				                           });

		Member member = memberRepository.findById(comment.getMember().getId())
                                        .orElseThrow(() -> {
                                        	logger.error("CommentServiceImpl deleteComment() NoSuchElementException memberId: {} ",comment.getMember().getId());
                                        	return new NoSuchElementException("회원이 존재하지 않습니다.");
                                        });

		// 삭제된 댓글은 신고 X
		if(comment.getStatus() == CommentStatus.DELETED) {
			logger.error("CommentServiceImpl reportComment() IllegalStateException : 삭제된 댓글은 신고할 수 없습니다.");
			throw new IllegalStateException("삭제된 댓글은 신고할 수 없습니다.");
		}

		// 작성자와 신고자가 같을경우 신고X
		if(comment.getMember().getId().equals(reporterId)) {
			logger.error("CommentServiceImpl reportComment() IllegalStateException : 본인이 본인 댓글을 신고할 수 없습니다. ");
			throw new IllegalStateException("자신이 자신의 댓글을 신고할 수 없습니다.");
		}

		// 댓글 신고 중복 방지(댓글 신고 테이블에 '해당'댓글을 신고한 회원이 존재하는지 여부 체크)
		boolean alreadyReported  = commentReportRepository.existsByCommentAndReporterId(comment, reporterId);

		if(alreadyReported) {
			logger.error("CommentServiceImpl IllegalStateException : 이미 신고한 댓글입니다.");
			throw new IllegalStateException("이미 신고한 댓글입니다.");
		}

		// 신고 저장
		CommentReport report = CommentReport.builder()
		                                    .comment(comment) // 신고할 댓글
		                                    .reporterId(reporterId) // 신고자의ID(PK)
		                                    .reason(reason) // 신고 이유
		                                    .build();

		commentReportRepository.save(report);

		// 신고 누적 수 증가
		comment.incrementReportCount();

		// 신고 갯수 가져오기
		Long reportCount = comment.getReportCount();

		CommentReportResponseDTO response = null;

		// 신고가 15번 당할시 상태 변경 (report_count(DB) == 15)
		// 그리고(AND(&&)), CommentStatus.ACTIVE
		if(Objects.equals(reportCount, this.reportThreshold) && comment.getStatus().equals(CommentStatus.ACTIVE)) {
			// 댓글 신고시 회원 등급 점수 차감
			member.benCommentScore();
			// 댓글 논리적 삭제
			comment.setStatus(CommentStatus.HIDDEN);
			// 해당 댓글 리액션 모두 삭제
			commentReactionRepository.deleteByComment(comment);
			// 신고로 인한 댓글 알림
			notificationServiceImpl.notifyCommentWarned(comment);
			CommentResponseDTO updatedCommentDto = CommentResponseDTO.fromEntity(comment, 0, 0, false, "");
			response = new CommentReportResponseDTO("",updatedCommentDto);
		}else {
			// 신고만 할시
			response = new CommentReportResponseDTO("댓글 신고가 접수되었습니다.",null);
		}

		logger.info("CommentServiceImpl reportComment() End");
		return response; 
	}

	// 전체 댓글 트리구조로 보여주기
	@Override
	@Transactional(readOnly =  true)
	public CommentPageResponseDTO getCommentsTreeByPost(Long postId, String sortBy, Pageable pageable) {

		logger.info("CommentServiceImpl getCommentsTreeByPost() Start");

		Post post = postRepository.findById(postId)
				                  .orElseThrow(() -> {
				                	  logger.error("CommentServiceImpl getCommentsTreeByPost() NoSuchElementException Error : {}");
				                	  return new NoSuchElementException("게시글이 존재하지 않습니다.");
				                  });

		List<CommentStatus> statuses = Arrays.asList(CommentStatus.ACTIVE,
				                                     CommentStatus.DELETED,
				                                     CommentStatus.HIDDEN); 


		// 모든 댓글 상태 리스트(삭제됨, 숨김, 대댓글포함)
		List<Comment> allComments = commentRepository.findByPostAndStatusIn(post, statuses);

		// 모든 댓글,대댓글 ID(Long) 추출
		List<Long> allCommentIds = allComments.stream()
				                              .map(comment -> comment.getCommentId())
				                              .collect(Collectors.toList());

		// 댓글 ID 리스트로 한번에 좋아요/싫어요 집계조회
		// ex) commentId | reactionType | Count
		// 		1			LIKE			3
		//		1			DISLIKe			1
		List<CommentReactionRepository.ReactionCountProjection> reactionCounts = commentReactionRepository.countReactionsByCommentIds(allCommentIds);

		// Map<commentId, Map<ReactionType, count>>
		// 댓글ID(commentId)의 '좋아요/싫어요(ReactionType)'의 총 갯수(count)
		// ex) map.get(1).get(ReactionType.LIKE) = 좋아요 총 5개 
		Map<Long, Map<PostReactionType, Long>> reactionCountMap = new HashMap<>();

		for(CommentReactionRepository.ReactionCountProjection rc :reactionCounts) {
			// Map.computeIfAbsent : 'Key(첫번쨰라미터)'가 있으면 'Value'를 반환,없으면 '두번쨰 파라미터 객체생성'
			// 여기서 'Key'가 없으면 HashMap이 생성되는데 <K,V>는 'Java'가 주변 코드를 살펴서 알아서 추론 
			// 즉, 새로 생성된 HashMap은 'Key'의 'Value'인 Map<ReactionType,Long>으로 추론하여 반환
			reactionCountMap.computeIfAbsent(rc.getCommentId(), k -> new HashMap<>())
			// put() 을 사용하여, 반환된 Map<ReactionType,Long>에 맞게 값을 덮어씀.
			.put(rc.getReactionType(), rc.getCount());
		}

		// Entity -> DTO 변환 시 자식 댓글까지 좋아요/싫어요 반영 재귀 호출 함수
		// Map<Long, CommentResponseDTO> dtoMap = new HashMap<>();
		Map<Long, CommentResponseDTO> dtoMap = new HashMap<>();

		for (Comment comment : allComments) {
		    CommentResponseDTO dto = convertCommentToDtoRecursive(comment, reactionCountMap);
		    dtoMap.put(comment.getCommentId(), dto);
		}

		// 루트 댓글 목록 준비 (CommentResponseDTO)
		// private List<CommentResponseDTO> childComments;
		List<CommentResponseDTO> rootComments = new ArrayList<>();

		// 부모 댓글만 rootComments에 추가 (최상위 댓글)
		for(Comment comment : allComments) {
			CommentResponseDTO dto = dtoMap.get(comment.getCommentId());
			// 'comment'의 'parentComment'가 'null'이면 최상위 댓글
			if(comment.getParentComment() == null) {
				// 루트 댓글(최상위 댓글), 
				rootComments.add(dto);
			}
		}

		// 'allComments'가 아닌 dtoMap으로 하는 이유는 'HashMap'으로 DTO 객체가 저장되어있으므로,
		// 접근이 빠르고, 트리구조에 적합 
		// HashMap<commentId,ResponseDTO> -> Stream<ResponseDTO> 원하는 자료구조형태로 변환 준비
		List<CommentResponseDTO> top3Pinned = dtoMap.values().stream()
															 // 'ResponseDTO'를 좋아요 30개 이상, 
															 // 좋아요 싫어요 차이가 10개보다 큰 댓글 필터링
														     .filter(responseDto -> { 
														    	int diff = responseDto.getLikeCount() - responseDto.getDislikeCount();
														    	return responseDto.getLikeCount() >= likeThreshold && diff >= netLikeThreshold;
														     })
														     // 'ResponseDTO'를 좋아요 - 싫어요 '

														     /* 'b'댓글의 좋아요 수와 'a'댓글의 좋아요 수를 비교 하여 (sorted),
														      *  Integer.compare(b.getLikeCount(), a.getLikeCount()))(내림차순)
														      *  Integer.compare(b.getLikeCount(), a.getLikeCount()) (오름차순)
														      *  결과가 양수면 'b'댓글이 앞 'a'댓글이 뒤, 음수면 'a'댓글이 앞 'b'댓글이 뒤 (내림차순)
														      */
														     // 상단 고정 3개 댓글 좋아요 내림차순 + 최신순 정렬
														     .sorted((a,b) -> {
														    	 int likeDiff = Integer.compare(b.getLikeCount(), a.getLikeCount());
														    	 if(likeDiff != 0) {
														    		 return likeDiff;
														    	 }else {
														        	// 만약 'Acomment'와 'Bcomment'가 '좋아요 수'가 같다면은
														        	// 최신순으로 정렬 메소드 호출한 'Bcomment'기준으로 'Acomment' 보다 최신이면 양수, 아니면 음수
														        	// 양수 : 앞 , 음수 : 뒤
														    		 return b.getCreatedAt().compareTo(a.getCreatedAt());
														    	 }
														     })
														     .limit(topLimit) // 내림차순으로 정렬된 것중에 '3개만'
														     .collect(Collectors.toList()); // Stream<ResponseDTO> -> List<ResponseDTO>

		// 인기글에 딸린 대댓글은 빈 리스트로 초기화
		for(CommentResponseDTO dto : top3Pinned ) {
			dto.setChildComments(Collections.emptyList());
		}

		// List<CommentResponseDTO> -> Stream<CommentResponseDTO) 원하는 자료구조로 변환 준비
		Set<Long> top3Ids = top3Pinned.stream()
									  // map : 타입 변환
									  // Stream<CommentResponseDTO> -> Stream<Long>(commentId)
				                      .map(commentResponseDto -> commentResponseDto.getCommentId())
				                      // 내림차순 순서유지를 위한 LinkedHashSet 변환
				                      .collect(Collectors.toCollection(() -> new LinkedHashSet<>()));

		// 상단 고정 댓글 'Pinned(true)' 작업
		for(CommentResponseDTO dto : dtoMap.values()) {
			if(top3Ids.contains(dto.getCommentId())) {
				// '내림차순'으로 정렬된 'top3Pinned'의 'top3Ids'에 포함된,
				// '댓글ID'라면 상단 고정을 위한 setPinned(true)
				dto.setPinned(true);
			}
		}

		// 상단 3개 좋아요순 댓글 + 나머지 댓글은 최신순
		// 상단 3개 좋아요 먼저 처리 ('List'는 순서 유지 때문에 먼저 상단 3개 댓글 부터 넣어줘야함)
		List<CommentResponseDTO> sortedRoot = new ArrayList<>();

		// 댓글 첫 페이지만 상단 3개 좋아요 보여주기
		int currentPage = pageable.getPageNumber();
		if(currentPage == 0) {
			sortedRoot.addAll(top3Pinned);
		}

		// 나머지 댓글 최신순 작업 (부모 댓글 먼저)
		List<CommentResponseDTO> restComments = rootComments.stream()
				                                            // Comparator.comparing(CommentResponseDTO :: getCreatedAt) 생성일자 기준 오름차순
				                                            // -> .reversed() 내림차순 -> sorted() 정렬
				                                            .sorted((a,b) -> {
				                                            	if("like".equalsIgnoreCase(sortBy)) {
				                                            		// 좋아요 내림차순 + 최신순
				                                            		int likeDiff = Integer.compare(b.getLikeCount(), a.getLikeCount());
				                                            		if(likeDiff !=0) {
				                                            			return likeDiff;
				                                            		}else {
				                                            			return b.getCreatedAt().compareTo(a.getCreatedAt());
				                                            		}
				                                            		
				                                            	}else {
				                                            		// 최신순(default)
				                                            		return b.getCreatedAt().compareTo(a.getCreatedAt());
				                                            	}
				                                            })
				                                            .collect(Collectors.toList());

		// 현재 상단 3개 좋아요순 댓글과 부모 댓글 최신순으로 정렬된 'List'
		sortedRoot.addAll(restComments);

		// 정렬된 'List'를 재귀 함수를 통해 자식 댓글 최신순으로 정렬
		sortChildComments(sortedRoot,true);

		CommentPageResponseDTO response = CommentPageResponseDTO.fromEntityToPage(sortedRoot, pageable);

		logger.info("CommentServiceImpl getCommentsTreeByPost() End");
		return response;
	}

	//*************************************************** Service End ***************************************************//

}
