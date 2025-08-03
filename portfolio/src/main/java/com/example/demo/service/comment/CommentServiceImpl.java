package com.example.demo.service.comment;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;

import com.example.demo.domain.comment.Comment;
import com.example.demo.domain.comment.commentenums.CommentStatus;
import com.example.demo.domain.comment.commentnotification.CommentNotification;
import com.example.demo.domain.comment.commentnotification.commentnotificationenums.CommentNotificationType;
import com.example.demo.domain.commentreport.CommentReport;
import com.example.demo.domain.post.Post;
import com.example.demo.domain.postreaction.postreactionenums.ReactionType;
import com.example.demo.dto.comment.CommentPageResponseDTO;
import com.example.demo.dto.comment.CommentRequestDTO;
import com.example.demo.dto.comment.CommentResponseDTO;
import com.example.demo.repository.comment.CommentRepository;
import com.example.demo.repository.commentreaction.CommentReactionRepository;
import com.example.demo.repository.commentreport.CommentReportRepository;
import com.example.demo.repository.post.PostRepository;
import com.example.demo.service.comment.commentnotification.NotificationService;
import com.example.demo.service.comment.commentnotification.NotificationServiceImpl;
import com.github.benmanes.caffeine.cache.RemovalCause;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final CommentReportRepository commentReportRepository;
    private final PostRepository postRepository;
    private final CommentReactionRepository commentReactionRepository;
    private final NotificationService notificationService;

    // 좋아요 기준
    @Value("${comment.topPinned.likeThreshold}")
    private int likeThreshold;

    // 댓글 상위 탑 3개
    @Value("${comment.topPinned.limit}")
    private int topLimit;

    // 신고 제한
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

    //*************************************************** Service START ***************************************************//

    // 댓글 생성 Service
	@Override
	public CommentResponseDTO createComment(CommentRequestDTO commentRequestDTO, Long authorId) {

		logger.info("CommentServiceImpl createComment() Start");

		// Request
		Long requestPostId = commentRequestDTO.getPostId();
		String requestContent = UriUtils.decode(commentRequestDTO.getContent(), StandardCharsets.UTF_8);
		Long requestParentCommentId = commentRequestDTO.getParentCommentId();

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
				                 .authorId(authorId)
				                 .content(requestContent)
				                 .reportCount(0L)
				                 .status(CommentStatus.ACTIVE)
				                 .isPinned(false)
				                 .build();

		// 'INSERT' 후 'Comment' Entity 반환
		Comment saved = commentRepository.save(comment);

		// 알림 타입 결정 : COMMENT (댓글), REPLY(대댓글)
		CommentNotificationType notificationType = null;

		// 알림을 받을 대상자 (게시글 작성자(자신제외), 부모댓글 작성자)
		Long receiverId = null;

		if(saved.getParentComment() == null) {
			// 만약 부모 댓글(일반 댓글)이라면, 게시글 작성자에게 알림 보내야됨
			// 게시글 작성자 ID
			receiverId = saved.getPost().getAuthorId();
			// 댓글(Comment)
			notificationType = CommentNotificationType.COMMENT;
		}else {
			// 만약 대댓글이라면, 부모 댓글 작성자에게 알림 보내기
			// 부모댓글(댓글)작성자 ID
			receiverId = saved.getParentComment().getAuthorId();
			notificationType = CommentNotificationType.REPLY;
		}

		// 게시글 작성자가 작성한 댓글은 알림 제외
		if(!receiverId.equals(authorId)) {
			// 댓글 작성자와 게시글 작성자가 다를시 알림 전송
			notificationService.createNotification(receiverId, 
					                               saved.getCommentId(), 
					                               notificationType, 
					                               saved.getContent());
		}

		logger.info("CommentServiceImpl createComment() End");
		return CommentResponseDTO.fromEntity(saved, 0, 0, false);
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

		if(!comment.getAuthorId().equals(authorId)) {
			logger.error("CommentServiceImpl updateComment() SecurityException Error : {}");
			throw new SecurityException("본인의 댓글만 수정할 수 있습니다.");
		}

		String requestContent = UriUtils.decode(content, StandardCharsets.UTF_8);

		comment.setContent(requestContent);

		int likeCount = commentReactionRepository.countByCommentAndReactionType(comment, ReactionType.LIKE);
		int dislikeCount = commentReactionRepository.countByCommentAndReactionType(comment, ReactionType.DISLIKE);

		logger.info("CommentServiceImpl updateComment() End");
		return CommentResponseDTO.fromEntity(comment, likeCount, dislikeCount, false);
	}

	// 댓글 삭제 Service
	@Override
	public CommentResponseDTO deleteComment(Long commentId, Long authorId) {

		logger.info("CommentServiceImpl deleteComment() Start");

		Comment comment = commentRepository.findById(commentId)
				                           .orElseThrow(() -> {
				                        	   logger.error("CommentServiceImpl deleteComment() NoSuchElementException Error : {}");
				                        	   return new NoSuchElementException("댓글이 존재하지 않습니다.");
				                           });

		if(!comment.getAuthorId().equals(authorId)) {
			logger.error("CommentServiceImpl deleteComment() SecurityException Error: {}");
			throw new SecurityException("본인의 댓글만 삭제할 수 있습니다.");
		}

		comment.setStatus(CommentStatus.DELETED);

	    // 좋아요/싫어요 수는 0 또는 실제 집계 조회
	    int likeCount = commentReactionRepository.countByCommentAndReactionType(comment, ReactionType.LIKE);
	    int dislikeCount = commentReactionRepository.countByCommentAndReactionType(comment, ReactionType.DISLIKE);

		logger.info("CommentServiceImpl deleteComment() End");
		return CommentResponseDTO.fromEntity(comment, likeCount, dislikeCount, false);
	}

	// 댓글 신고 Service
	@Override
	public String reportComment(Long commentId, Long reporterId, String reason) {

		logger.info("CommentServiceImpl reportComment() Start");

		Comment comment = commentRepository.findById(commentId)
				                           .orElseThrow(() -> {
				                        	   logger.error("CommentServiceImpl reportComment() NoSuchElementException : 댓글이 존재하지 않습니다.");
				                        	   return new NoSuchElementException("댓글이 존재하지 않습니다.");
				                           });

		if(comment.getAuthorId().equals(reporterId)) {
			throw new IllegalStateException("본인이 본인 댓글을 신고할 수 없습니다.");
		}

		// 댓글 신고 중복 방지
		boolean alreadyReported  = commentReportRepository.existsByCommentAndReporterId(comment, reporterId);

		if(alreadyReported) {
			throw new IllegalStateException("이미 신고한 댓글입니다.");
		}

		// 신고 저장
		CommentReport report = CommentReport.builder()
		                                    .comment(comment) // 신고할 댓글
		                                    .reporterId(reporterId) // 신고자의 id(PK)
		                                    .reason(reason) // 신고 이유
		                                    .build();

		CommentReport saved = commentReportRepository.save(report);

		// 댓글 작성자 에게 알림 보내기
		Long receiverId = comment.getAuthorId();

		// 댓글 작성자 ID가 신고한 reporterId와 같지 않다면
		if(!receiverId.equals(saved.getReporterId())) {
			// 신고 알림 댓글 작성자에게 보내기
			String content = "회원님의 댓글이 신고되었습니다.\n사유 :" + reason;

			notificationService.createNotification(receiverId,
					                               comment.getCommentId(), 
					                               CommentNotificationType.REPORT, 
					                               content);

		}

		// 신고 누적 수 증가
		comment.incrementReportCount();

		// 신고 갯수 가져오기
		Long reportCount = comment.getReportCount();

		// 신고가 15번 당할시 상태 변경
		if(reportCount >= this.reportThreshold) {
			comment.setStatus(CommentStatus.HIDDEN);
		}

		logger.info("CommentServiceImpl reportComment() End");
		return "댓글 신고가 접수되었습니다."; 
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

		// 변경(25.08.03) : 부모 댓글만 페이지 단위로 조회
		// '게시글'에서 'ParentComment'가 'null'이면 부모 댓글
		Page<Comment> parentCommentsPage = commentRepository.findByPostAndParentCommentIsNull(post, pageable);
		// 부모 댓글 가져오기
		List<Comment> parentComments = parentCommentsPage.getContent();

		// 추가(25.08.03) : 부모 댓글 ID로 가져오기
		List<Long> parentCommentIds = parentComments.stream()
				                                    .map(comment -> comment.getCommentId())
				                                    .collect(Collectors.toList());

		// 추가(25.08.03) : 부모 댓글 ID로 자식댓글 'IN'으로 한번에 조회하기
		List<Comment> childComments = commentRepository.findByParentCommentCommentIdIn(parentCommentIds);

		// 추가(25.08.03) : '좋아요/싫오요' 집계시 부모 댓글과 자식 댓글을 모두 포함
		// 부모, 자식 댓글 더할 댓글 List
		List<Comment> allComments = new ArrayList<>();
				      allComments.addAll(parentComments);
				      allComments.addAll(childComments);

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
		Map<Long, Map<ReactionType, Long>> reactionCountMap = new HashMap<>();

		for(CommentReactionRepository.ReactionCountProjection rc :reactionCounts) {
			// Map.computeIfAbsent : 'Key(첫번쨰라미터)'가 있으면 'Value'를 반환,없으면 '두번쨰 파라미터 객체생성'
			// 여기서 'Key'가 없으면 HashMap이 생성되는데 <K,V>는 Java가 주변 코드를 살펴서 알아서 추론 
			// 즉, 새로 생성된 HashMap은 'Key'의 'Value'인 Map<ReactionType,Long>으로 추론하여 반환
			reactionCountMap.computeIfAbsent(rc.getCommentId(), k -> new HashMap<>())
			// put() 을 사용하여, 반환된 Map<ReactionType,Long>에 맞게 값을 덮어씀.
			.put(rc.getReactionType(), rc.getCount());
		}

		// 모든 댓글을 DTO로 변환 및 ID 기준 HashMap 생성(HashCode로 빠른접근 가능)
		Map<Long, CommentResponseDTO> dtoMap = new HashMap<>();

		// 준비 단계 Entity -> DTO변환 -> new ArrayList<>()(setChildComments)
		for(Comment comment : allComments) {

			Map<ReactionType, Long> counts = reactionCountMap.getOrDefault(comment.getCommentId(), Collections.emptyMap());
			int likeCount = counts.getOrDefault(ReactionType.LIKE, 0L).intValue();
	        int dislikeCount = counts.getOrDefault(ReactionType.DISLIKE, 0L).intValue();

	        // Entity -> DTO
	        CommentResponseDTO dto = CommentResponseDTO.fromEntity(comment, likeCount, dislikeCount, false);

	        // List<Comment> comments = null -> List<Comment> comments = new ArrayList<>();
	        // 실제 객체를 만들어줘서, 'null'을 add() 가능
	        dto.setChildComments(new ArrayList<>());
	        dtoMap.put(comment.getCommentId(), dto);
		}

		// 루트 댓글 목록 준비 (CommentResponseDTO)
		// private List<CommentResponseDTO> childComments;
		List<CommentResponseDTO> rootComments = new ArrayList<>();

		// 부모 댓글이 있으면 부모 DTO의 childComments에 추가, 없으면 root 목록에 추가
		for(Comment comment : allComments) {
			CommentResponseDTO dto = dtoMap.get(comment.getCommentId());
			// 'comment'의 'parentComment'가 'null'이면 최상위 댓글
			if(comment.getParentComment() == null) {
				// 루트 댓글(최상위 댓글)
				rootComments.add(dto);
			}else {
				// 대댓글
				// 부모 댓글의 'CommentResponseDTO' 가져오기
				CommentResponseDTO parentDto = dtoMap.get(comment.getParentComment().getCommentId());
				// parentDto 유효값 체크
				if(parentDto != null) {
					// 부모 댓글에 자식 'CommentResponseDTO' add
					parentDto.getChildComments().add(dto);
				}
				
			}
			
		}

		// 'allComments'가 아닌 dtoMap으로 하는 이유는 'HashMap'으로 DTO 객체가 저장되어있으므로,
		// 접근이 빠르고, 트리구조에 적합 
		// HashMap<commentId,ResponseDTO> -> Stream<ResponseDTO> 원하는 자료구조형태로 변환 준비
		List<CommentResponseDTO> top3Pinned = dtoMap.values().stream()
															 // 'ResponseDTO'를 좋아요 30개 이상만 필터
														     .filter(responseDto -> responseDto.getLikeCount() >= likeThreshold)

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
		List<CommentResponseDTO> sortedRoot = new ArrayList<>(top3Pinned);

		// 나머지 댓글 최신순 작업 (부모 댓글 먼저)
		List<CommentResponseDTO> restComments = rootComments.stream()
															// 'top3Ids'에 포함되지 않은 나머지 댓글 필터링
				                                            //.filter(responseDto -> !top3Ids.contains(responseDto.getCommentId()))
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

		CommentPageResponseDTO response = CommentPageResponseDTO.builder()
                                                                .comments(sortedRoot)
                                                                .pageNumber(parentCommentsPage.getNumber())
                                                                .pageSize(parentCommentsPage.getSize())
                                                                .totalElements(parentCommentsPage.getTotalElements())
                                                                .totalPages(parentCommentsPage.getTotalPages())
                                                                .build();

		logger.info("CommentServiceImpl getCommentsTreeByPost() End");
		return response;
	}

	//*************************************************** Service End ***************************************************//

}
