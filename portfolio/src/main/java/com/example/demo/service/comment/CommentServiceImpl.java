package com.example.demo.service.comment;

import java.nio.charset.StandardCharsets;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
import com.example.demo.dto.comment.CommentGoPageResponseDTO;
import com.example.demo.dto.comment.CommentListResponseDTO;
import com.example.demo.dto.comment.CommentMyPageResponseDTO;
import com.example.demo.dto.comment.CommentPageResponseDTO;
import com.example.demo.dto.comment.CommentRequestDTO;
import com.example.demo.dto.comment.CommentResponseDTO;
import com.example.demo.dto.comment.commentreport.CommentReportResponseDTO;
import com.example.demo.repository.comment.CommentRepository;
import com.example.demo.repository.comment.commentreaction.CommentReactionRepository;
import com.example.demo.repository.comment.commentreaction.CommentReactionRepository.ReactionCountProjection;
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
    

    // 게시글별 top3 인기댓글 ID 캐시 (전역처럼 사용)
    private static final Map<Long, Set<Long>> postTop3IdsCache = new ConcurrentHashMap<>();


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
    private void sortChildComments(List<CommentResponseDTO> comments, boolean isRoot, String sortBy) {

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

    		List<CommentResponseDTO> notPinnedComments = comments.stream()
    			                                                 .filter(comment -> !comment.isPinned())
    			                                                 .sorted((a,b) -> {
    			                                                	 if ("like".equalsIgnoreCase(sortBy)) {
    			                                                		 int likeDiff = Integer.compare(b.getLikeCount(), a.getLikeCount());
    			                                                		 if (likeDiff != 0) return likeDiff;
    			                                                		 return b.getCreatedAt().compareTo(a.getCreatedAt());
    			                                                	 } else if ("recent".equalsIgnoreCase(sortBy)) {
    			                                                		 // 고정되지 않은 대댓글 최신순 정렬 ('b'댓글 생성기준으로 비교하면 내림차순)
    			                                                		 return b.getCreatedAt().compareTo(a.getCreatedAt());
    			                                                	 } else {
    			                                                		 // 고정되지 않은 대댓글 오름차순 정렬(normal)
    			                                                		 return a.getCreatedAt().compareTo(b.getCreatedAt());
    			                                                	 }
    			                                                 })
    			                                                 .collect(Collectors.toList());
  
    		// '자식 댓글'들의 정렬 순서를 초기화 (대댓글, 대댓글, 대댓글(상단 3개 인기댓글))
    		comments.clear();
    		// '자식 댓글'중에 '고정된'댓글을 먼저 추가해 보여주고 (대댓글(상단 3개 인기댓글)
    		comments.addAll(pinnedComments); 
    		// 나머지 '자식 댓글'을 추가해서 보여줌 (대댓글(고정, 대댓글, 대댓글)
    		comments.addAll(notPinnedComments);
    	}

    	for(CommentResponseDTO comment : comments) {
    		// 정렬이 된 ''부모 댓글'의 'getChildComments()'를 사용하여 '자식댓글(대댓글)'들을 꺼내서 던짐
    		sortChildComments(comment.getChildComments(),false, sortBy);
    	}
    }

  //*************************************************** Helper Method START ***************************************************//
    private CommentResponseDTO convertCommentToDtoRecursive(Comment comment, Map<Long, Map<PostReactionType, Long>> reactionCountMap) {

    	logger.info("CommentServiceImpl convertCommentToDtoRecursive() Start");
    	// 리액션(좋아요, 싫어요) 꺼내기
    	Map<PostReactionType, Long> counts = reactionCountMap.getOrDefault(comment.getCommentId(), Collections.emptyMap());

    	// 좋아요 갯수 가져오기 없으면 '0'반환
    	int likeCount = counts.getOrDefault(PostReactionType.LIKE, 0L).intValue();
    	// 싫어요 갯수 가져오기 없으면 '0'반환
        int dislikeCount = counts.getOrDefault(PostReactionType.DISLIKE, 0L).intValue();

        String memberNickname = comment.getMember().getNickname();

        // 댓글 DTO로 반환
        CommentResponseDTO dto = CommentResponseDTO.fromEntity(comment, 
        		                                               likeCount, 
        		                                               dislikeCount, 
        		                                               false, 
        		                                               memberNickname);

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
        return new CommentResponseDTO(dto);
    }

    // 나머지 댓글 재귀로 pinned 작업
    private void applyPinnedRecursive(CommentResponseDTO dto, Set<Long> top3Ids) {
        // 현재 댓글이 top3Ids 안에 있으면 pinned 처리
        if (top3Ids.contains(dto.getCommentId())) {
            dto.setPinned(true);
        }

        // 자식 댓글이 있으면 재귀 호출
        if (dto.getChildComments() != null) {
            for (CommentResponseDTO child : dto.getChildComments()) {
                applyPinnedRecursive(child, top3Ids);
            }
        }
    }

    // 최상위 댓글 재귀로 찾기
    private Comment findRootComment(Comment comment) {
        while (comment.getParentComment() != null) {
            comment = comment.getParentComment();
        }
        return comment;
    }

    //*************************************************** Helper Method End ***************************************************//

    //*************************************************** Service START ***************************************************//

    // 댓글 생성 Service
	@Override
	public CommentResponseDTO createComment(CommentRequestDTO commentRequestDTO, Long authorId) {

		logger.info("CommentServiceImpl createComment() Start");

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

		// 댓글 or 대댓글 유효성 검사를 위한 변수 (Null = 댓글(부모(최상위), NotNull = 대댓글,대대댓글(자식)  
		Comment parentComment = null;

		// 대댓글 유효 검사
		// requestParentCommentId == null 이면 부모 댓글 이므로 분기 처리 X , 
		// != null 이면 대댓글 이므로 분기 처리
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


		// 'INSERT' 후 'Comment' Entity 반환
		Comment saved = commentRepository.save(comment);

	    // 부모 댓글에 자식 댓글 추가 (대댓글인 경우에만)
	    if (parentComment != null) {
	    	parentComment.getChildComments().add(saved); // 자식 댓글을 부모 댓글에 추가
	        commentRepository.saveAndFlush(parentComment); // 부모 댓글 업데이트
	    }

	    // 댓글이 INSERT 된 후 등급 올리기 (자기 자신 제외)
	    // "INSERT"된 댓글작성자ID와 게시글의작성자ID가 같다면은 자기 게시글의 댓글이므로, 등급 점수 올리기X
	    boolean isAuthorOwnPost = saved.getPost().getAuthor().getId().equals(authorId);
	    // saved.getParentComment() != null 대댓글 이므로,
	    // 대댓글(자식댓글)의 작성자가 루트댓글(부모댓글)의 작성자가 같다면은 본인 댓글에 대댓글을 작성한것이므로, 등급점수 올리기X 
	    boolean isAuthorOwnParentComment = saved.getParentComment() != null 
	                                       && saved.getParentComment().getMember().getId().equals(authorId);

	    // 자기 게시글에 댓글 작성 OR 자기 댓글에 대댓글 작성은 점수 제외
	    if (!isAuthorOwnPost && !isAuthorOwnParentComment) {
			// 댓글이 'INSERT' 된후 등급을 올림
			// 만약 댓글은 올라갔지만, 중간에 트랜잭션이 종료된다면,
			// '@Transactional'어노테이션에 의해서 등급 점수가 롤백되므로 안전함. 
			member.addCommentScore(); // 등급 점수 올리기
	    }


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



		// 수정 요청 댓글과 기존의 DB댓글과 다를경우에만 업데이트
		if(!content.equals(dbComment)) {
			comment.setContent(content);
			dbSetting = true;
		}

		// 만약 DB에 한번이라도 업데이트가 이뤄지지 않았다면,
		if(dbSetting == false) {
			// 예외 처리로 조기 종료
			logger.error("CommentServiceImpl updateComment() IllegalArgumentException : 잘못된 접근 입니다.");
			throw new IllegalArgumentException("댓글이 수정 되지 않았습니다.");
		}

		int likeCount = commentReactionRepository.countByCommentAndReactionType(comment, PostReactionType.LIKE);
		int dislikeCount = commentReactionRepository.countByCommentAndReactionType(comment, PostReactionType.DISLIKE);

		logger.info("CommentServiceImpl updateComment() End");
		return new CommentResponseDTO(CommentResponseDTO.fromEntity(comment, likeCount, dislikeCount, false, authorNickname));
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
		return new CommentResponseDTO(CommentResponseDTO.fromEntity(comment, 0, 0, false, ""));
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

		// 신고가 50번 당할시 (report_count(DB) == 50)
		// 그리고(AND(&&)), CommentStatus.ACTIVE 상태 변경(HIDDEN)
		if(Objects.equals(reportCount, this.reportThreshold) && comment.getStatus().equals(CommentStatus.ACTIVE)) {
			// 댓글 신고시 회원 등급 점수 차감
			member.benCommentScore();
			// 댓글 논리적 삭제
			comment.setStatus(CommentStatus.HIDDEN);
			// 해당 댓글 리액션 모두 삭제
			commentReactionRepository.deleteByComment(comment);
			// 신고로 인한 댓글 알림
			notificationServiceImpl.notifyCommentWarned(comment);
			CommentResponseDTO updatedCommentDto =  new CommentResponseDTO(CommentResponseDTO.fromEntity(comment, 0, 0, false, ""));;
			response = new CommentReportResponseDTO("신고가 누적되어 삭제되었습니다.",updatedCommentDto);
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
		List<Comment> allComments = commentRepository.findByPostWithStatusesDesc(post, statuses);
		
		// ACTIVE 상태 댓글만 조회 (CommentStatus.ACTIVE를 'List'로 변경)
		List<Comment> activeComments = commentRepository.findByPostWithStatusesDesc(post, Collections.singletonList(CommentStatus.ACTIVE));
		long totalActiveComments = activeComments.size();

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

		// 첫 페이지에 보여줄 인기댓글 처리
		List<CommentResponseDTO> top3Pinned = Collections.emptyList();
		// 인기댓들, 나머지 댓글에 'pin' 작업을 위한 변수
		Set<Long> top3Ids = null;

		int currentPage = pageable.getPageNumber();
		if(currentPage == 0) {
			// Entity -> DTO 변환 시 자식 댓글까지 좋아요/싫어요 반영 재귀 호출 함수
			// Map<Long, CommentResponseDTO> dtoMap = new HashMap<>();
			// 상위 인기 댓글 3개 뽑기위한 변수 
			Map<Long, CommentResponseDTO> dtoMap = new HashMap<>();

			// 상위 인기 댓글 3개 뽑기위한 작업
			for (Comment comment : allComments) {
			    CommentResponseDTO dto = convertCommentToDtoRecursive(comment, reactionCountMap);
			    dtoMap.put(comment.getCommentId(), dto);
			}

			// 'allComments'가 아닌 dtoMap으로 하는 이유는 'HashMap'으로 DTO 객체가 저장되어있으므로,
			// 접근이 빠르고, 트리구조에 적합 
			// HashMap<commentId,ResponseDTO> -> Stream<ResponseDTO> 원하는 자료구조형태로 변환 준비
			top3Pinned = dtoMap.values()
							   .stream()
			                   // ACTIVE 상태인 댓글만
			                   .filter(responseDto -> responseDto.getStatus() == CommentStatus.ACTIVE)
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

			// 상단 인기 댓글에 딸린 대댓글은 빈 리스트로 초기화
			// 주의!! 일반댓글이 '같은 주소'의 'HashMap<>()'을 사용한다면은
			// 일반댓글과 인기댓글들의 자식 댓글을 비우기때문에, 따로 HashMap 객체를 생성해서 분리 
			for(CommentResponseDTO dto : top3Pinned ) {
				dto.setChildComments(Collections.emptyList());
			}

			// List<CommentResponseDTO> -> Stream<CommentResponseDTO) 원하는 자료구조로 변환 준비
			top3Ids = top3Pinned.stream()
							    // map : 타입 변환
							    // Stream<CommentResponseDTO> -> Stream<Long>(commentId)
		                        .map(commentResponseDto -> commentResponseDto.getCommentId())
		                        // 내림차순 순서유지를 위한 LinkedHashSet 변환
		                        .collect(Collectors.toCollection(() -> new LinkedHashSet<>()));
			// 첫 페이지 처리 후 저장
			if (top3Ids != null && !top3Ids.isEmpty()) {
			    postTop3IdsCache.put(postId, top3Ids);
			} else {
			    postTop3IdsCache.put(postId, Collections.emptySet());
			}

			// 상단 고정 댓글 'Pinned(true)' 작업
			for(CommentResponseDTO dto : dtoMap.values()) {
				if(top3Ids.contains(dto.getCommentId())) {
					// '내림차순'으로 정렬된 'top3Pinned'의 'top3Ids'에 포함된,
					// '댓글ID'라면 상단 고정을 위한 setPinned(true)
					dto.setPinned(true);
				}
			}
		}
		// 나머지 댓글을 위한 변수
		Map<Long, CommentResponseDTO> normalDtoMap = new HashMap<>();
		// 나머지 댓글 자식 댓글 셋팅
		for(Comment comment : allComments) {
		    CommentResponseDTO normalDto = convertCommentToDtoRecursive(comment, reactionCountMap);
		    normalDtoMap.put(comment.getCommentId(), normalDto);
		}

		List<CommentResponseDTO> rootComments = normalDtoMap.values()
				                                            .stream()
				                                            .filter(Objects::nonNull)
			    									        // 루트 댓글 필터링 (부모 댓글이 null인 댓글만)
				                                            .filter(dto -> dto.getParentCommentId() == null)
				                                            // 리스트로 수집
				                                            .collect(Collectors.toList());

		// 나머지 댓글은 최신순
		// 상단 3개 좋아요 먼저 처리 ('List'는 순서 유지 때문에 먼저 상단 3개 댓글 부터 넣어줘야함)
		List<CommentResponseDTO> sortedRoot = new ArrayList<>();

		// 나머지 댓글 최신순 작업 (부모(루트) 댓글 먼저)
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
				                                            		
				                                            	}else if("recent".equalsIgnoreCase(sortBy)) {
				                                            		return b.getCreatedAt().compareTo(a.getCreatedAt());
				                                            	}else {
				                                            		return a.getCreatedAt().compareTo(b.getCreatedAt());
				                                            	}
				                                            })
				                                            .collect(Collectors.toList());

		// 상단이 아닌 인기 댓글 처리
		Set<Long> cachedTop3Ids = postTop3IdsCache.getOrDefault(postId, Collections.emptySet());
		if (!cachedTop3Ids.isEmpty()) {
		    // pinned 처리
		    for (CommentResponseDTO dto : restComments) {
		        applyPinnedRecursive(dto, cachedTop3Ids);
		    }
		}

		// 현재 상단 3개 좋아요순 댓글과 부모 댓글 최신순으로 정렬된 'List'
		sortedRoot.addAll(restComments);
		// 정렬된 상단 3개 좋아요순 댓글과 부모 댓글 최신순으로 정렬된 'List'를 재귀 함수를 통해 자식 댓글 최신순으로 정렬
		sortChildComments(sortedRoot,true, sortBy);

		CommentPageResponseDTO response = CommentPageResponseDTO.fromEntityToPage(top3Pinned,sortedRoot, pageable,totalActiveComments);

		logger.info("CommentServiceImpl getCommentsTreeByPost() End");
		return response;
	}

	// 내정보 댓글 보기
	@Override
	@Transactional(readOnly =  true)
	public CommentMyPageResponseDTO getMyComments(Long memberId, Pageable pageable) {
	    logger.info("CommentServiceImpl getMyComments() Start");

	    // 1. 내 댓글 조회 (작성자 + 상태 ACTIVE)
	    Page<Comment> commentPage = commentRepository.findByAuthor(memberId, CommentStatus.ACTIVE, pageable);

		// 내 댓글 ID(Long) 추출
		List<Long> allCommentIds = commentPage.stream()
				                              .map(comment -> comment.getCommentId())
				                              .collect(Collectors.toList());
		
		// 댓글 ID 리스트로 한번에 좋아요/싫어요 집계조회
		// ex) commentId | reactionType | Count
		// 		1			LIKE			3
		//		1			DISLIKe			1
		List<CommentReactionRepository.ReactionCountProjection> reactionCounts = commentReactionRepository.countReactionsByCommentIds(allCommentIds);
	    
		// commentId -> likeCount 맵으로 변환
		Map<Long, Integer> likeCountMap = reactionCounts.stream()
			                                            .filter(r -> r.getReactionType() == PostReactionType.LIKE)
			                                            .collect(Collectors.toMap(
			                                            			ReactionCountProjection::getCommentId,
			                                            			r -> r.getCount().intValue(),  // Long -> int
			                                            			Integer::sum
			                                            ));

		// DTO 변환
		List<CommentListResponseDTO> dtoList = commentPage.getContent()
				                                          .stream()
				                                          .map(comment -> {
				                                        	  	int likeCount = likeCountMap.getOrDefault(comment.getCommentId(), 0);
				                                        	  	return CommentListResponseDTO.fromEntity(comment, likeCount);
				                                          })
				                                          .toList();

	    // 3. No 계산 (최신 댓글이 가장 높은 No, 최소값 1)
	    long startNo = Math.max(commentPage.getTotalElements() - (long)pageable.getPageNumber() * pageable.getPageSize(), 1);
	    for (int i = 0; i < dtoList.size(); i++) {
	        long no = startNo - i;
	        dtoList.get(i).setNo(Math.max(no, 1));
	    }

	    // 4. Page -> DTO 변환
	    Page<CommentListResponseDTO> dtoPage = new PageImpl<>(dtoList, pageable, commentPage.getTotalElements());
	    CommentMyPageResponseDTO responseDTO = CommentMyPageResponseDTO.fromPage(dtoPage);

	    logger.info("CommentServiceImpl getMyComments() End");
	    return responseDTO;
	}

	// 댓글 바로가기 서비스
	public CommentGoPageResponseDTO getCommentPage(Long commentId, String sortBy, int pageSize) {

		logger.info("CommentServiceImpl getCommentPage() Start");
	    // 1. 댓글 조회
	    Comment comment = commentRepository.findById(commentId)
	                                       .orElseThrow(() -> new NoSuchElementException("댓글이 존재하지 않습니다."));

	    Long postId = comment.getPost().getPostId();
	    
	    // 2. 루트 댓글 찾기
	    Comment rootComment = findRootComment(comment);

	    // 3. 모든 댓글 가져오기(getCommentsTreeByPost 서비스 사용)
	    CommentPageResponseDTO commentPage = this.getCommentsTreeByPost(postId, sortBy, PageRequest.of(0, Integer.MAX_VALUE));

	    // 4. 루트 댓글 목록 가져오기
	    List<CommentResponseDTO> rootComments = commentPage.getComments() //Page -> List 로 변경
	    												   .stream()// List -> Stream( 데이터 가공준비 )
	    												   .filter(c -> c.getParentCommentId() == null) // 루트 댓글만
	    												   .toList();

	    // 5. 해당 루트 댓글의 인덱스 찾기
	    int index = -1;
	    for (int i = 0; i < rootComments.size(); i++) {
	    	// 댓글 페이지, 위치를 반환하기위한 index 찾기 
	        if (rootComments.get(i).getCommentId().equals(rootComment.getCommentId())) {
	            index = i;
	            break;
	        }
	    }
	    if (index == -1) {
	    	throw new IllegalStateException("부모 댓글을 찾을 수 없습니다.");
	    }

	    // 6. 페이지 번호, 위치 계산
	    int pageNumber = (index / pageSize);       // 페이지 번호
	    int positionInPage = (index % pageSize) + 1;   // 페이지 내 위치
	    int totalPages = (int) Math.ceil((double) rootComments.size() / pageSize); // 총페이지

	    logger.info("CommentServiceImpl getCommentPage() End");
	    // 7. 결과 반환
	    return new CommentGoPageResponseDTO(commentId, pageNumber, totalPages, positionInPage);
	}

	//*************************************************** Service End ***************************************************//

}
