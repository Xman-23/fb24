// 타이머 변수 
var debounceTimer;
var token = localStorage.getItem('accessToken'); // 현재 액세스 토큰 가져오기
//***************************************** ID Start *************************************************************
// 도메인에서 '/'기준으로 배열화[] 5 -1  
const pathParts = window.location.pathname.split("/");
// 도메인 배열에서 2번째 인덱스 boardId 가져오기
//const boardId = Number(pathParts[pathParts.lenth -3]);
// 도메인에서 마지막 인덱스 postId 가져오기
const postId = Number(pathParts[pathParts.length - 1]);
// 공지게시판 BoardId
//const noticeBoard = [1];
// 부모게시판 BoardId
//const parentBoardIds = [9, 14, 15, 20];
//***************************************** ID End *************************************************************

//***************************************** 게시글 Start ************************************************************* 
const main_post = $("#main_post");
//***************************************** 게시글 End *************************************************************

//*****************************************No Comment Start************************************************************* 
const no_main_popularList = "메인 인기 게시글이 없습니다.";
const no_popularList = "인기 게시글이 없습니다.";
const no_normalList = "게시글이 없습니다.";
const no_searchList = "검색 결과가 없습니다.";
const no_fin_noticeList ="고정된 공지 게시글이 없습니다.";
const no_noticeList ="공지 게시글이 없습니다.";
//*****************************************No Comment End*************************************************************

//*****************************************Function Start*************************************************************

//*****************************************Post Function Start*************************************************************

//*****************************************Post Function End*************************************************************

//*****************************************Board Function Start*************************************************************
// 게시글이 없는 경우 만들어줄 '<li></li>'태그
function no_posts_tag(string) {

	var no_posts_html = `
							<li class= "no-posts">
								${string}
							</li>
						`
	return no_posts_html;

}

// 게시글 유효성 체크
function check_posts(posts) {
	return !posts || posts.length === 0;
}

// 이미지 영역(post_images_download)보여주기 메인, 부모 , 자식 게시글 공용으로 사용
function shwo_image_download(imageUrls) {

	var post_images_download = $("#post_images_download");
	post_images_download.empty(); //기존 내뇽 초기화

	if(imageUrls.length ===0) {
		post_images_download.hide();
		return;
	}
	var image_span =`
						<span id='image_downlad_span'>
							이미지: 
						</span>
					`;
	post_images_download.append(image_span);
    imageUrls.forEach(function(url) {
		//split(/\#|\?/) = 확장자 뒤에 오는 query(?)/hash(#)배열화 제거, [0]= 순수 이미지 이름 가져오기
        var fileName = url.split("/images/").pop().split(/\#|\?/)[0]; 
        var link = `<a href="${url}" download="${fileName}" style="margin: 0 5px;">${fileName}</a>`
        post_images_download.append(link);
    });

    post_images_download.show(); // 부모 영역 표시
}
//*****************************************Board Function End*************************************************************
//*****************************************Function End*************************************************************

//*****************************************API Start******************************************************************
//*****************************************Post API Start******************************************************************
// 조회수 증가 API (메인, 부모 , 자식 게시글 공용으로 사용)
function view_count_increment(postId) {
	// ajax옵션 객체 셋팅
	$.ajax({
		url: `/posts/${postId}/view`,
		method: "PATCH",
		success: function() {
			getPostDetail(postId);
		},
		error: function(err) {
			console.log("조회수 증가 실패: " + err.responseText);
		}
	})
}

// 상세 게시글 좋아요/싫어요 클릭 API (메인, 부모 , 자식 게시글 공용으로 사용)
function reaction_api(postId,token) {

	$("#post_btn_like, #post_btn_dislike").off("click").on("click", function() {

		if(!token) {
			if(confirm("로그인이 필요한 기능입니다. 로그인하시겠습니까?")) {
				window.location.href ="/signin";
			}
			return;	
		}

		// 좋아요, 싫어요 버튼 클릭시 해당 객체 태그 ($(this)) 가져와서, 해당 태그의 'id' 삼항연산자로 비교
	    var reactionType = $(this).attr("id") === "post_btn_like" ? "LIKE" : "DISLIKE";

	    ajaxWithToken({
	        url: `/postreactions/${postId}/reaction`,
	        type: "POST",
	        contentType: "application/json",
	        data: JSON.stringify({ reactionType: reactionType }),
	        success: function(response) {
				console.log(response);
	            // 서버 응답 DTO(PostReactionResponseDTO) 값으로 UI 업데이트
	            $("#post_likes").text(response.likeCount);
	            $("#post_dislikes").text(response.dislikeCount);

	            // 내가 현재 누른 상태 강조 (LIKE, DISLIKE, null)
	            if (response.userPostReactionType === "LIKE") {
	                $("#post_btn_like").css("font-weight", "bold");
	                $("#post_btn_dislike").css("font-weight", "normal");
	            } else if (response.userPostReactionType === "DISLIKE") {
	                $("#post_btn_dislike").css("font-weight", "bold");
	                $("#post_btn_like").css("font-weight", "normal");
	            } else {
	                // 취소된 경우
	                $("#post_btn_like, #post_btn_dislike").css("font-weight", "normal");
	            }
	        },
	        error: function(xhr) {
	            alert("반응 처리 실패: " + xhr.responseText);
	        }
	    });
	});
}
//*****************************************Post API End******************************************************************

//*****************************************Comment API Start******************************************************************
// 1. 댓글 가져오기 (통합 검색 부모, 자식 게시글에 공통으로 빼기)
function loadComments(postId, sortBy = 'normal', page = 0) {
    $.ajax({
        url: `/comments/post/${postId}?sortBy=${sortBy}&page=${page}`,
        type: 'GET',
        success: function(response) {
			console.log(response);
			$("#post_comments").empty();
			$('#post_comments').text(`${response.activeTotalElements}`);
			if(page === 0) {
				// 상단 댓글 렌더링
				renderPopularComments(response.popularComments,$("#popular_comments_list"));
			}

            renderComments(response.comments, $('#comments_list'));
        },
        error: function(err) {
            console.error(err);
            alert("댓글을 불러오는 중 에러가 발생했습니다.");
        }
    });
}

//*****************************************인기 댓글 처리 Start******************************************************************* */
function renderPopularComments(popularComments, popular_comments_list) {

	if(!popularComments || popularComments.length === 0) {
		return;
	}

	popular_comments_list.empty();
	// 인기 댓글 섹션 제목 추가
	popularComments.forEach(popularComment => {
		var popularElem = createPopularCommentElem(popularComment);
		popular_comments_list.append(popularElem);
	})
}

function createPopularCommentElem(comment) {
	// 댓글은 여러개의 댓글이존재할 수 있으므로 "id"가 아닌 "class"로 명시해야한다
	// id는 HTML에서 단 한개만 존재해야마지만 클래스는 여러개 존재해도 되기때문이다.
	// 그리고 이렇게 공통 클래스를 명시해두면 댓글마다 이벤트를 걸 수 있으므로, 불필요한 로직이 필요없다.
	var popularCommentDiv = $(`
							    <div class="popular_comment" data_popular_comment_id="${comment.commentId}">
							        <div class="popular_comment_header">
										<div class="popular_comment_info">
							            	<span class="popular_comment_hot">🔥</span>
							            	<span class="popular_comment_author">${comment.authorNickname}</span>
							            	<span class="popular_comment_created">${comment.updatedAgo || comment.createdAt}</span>
										</div>
							            <span class="popular_comment_actions">
							                <button id=popular_comment_btn_reply_${comment.commentId} class="popular_comment_btn_reply">답글</button>
							                <button id=popular_comment_btn_edit_${comment.commentId} class="popular_comment_btn_edit">수정</button>
							                <button id=popular_comment_btn_report_${comment.commentId} class="popular_comment_btn_report">신고</button>
							                <button id=popular_comment_btn_like_${comment.commentId} class="popular_comment_btn_like">👍 <span class="popular_comment_like_count">${comment.likeCount}</span></button>
							                <button id=popular_comment_btn_dislike_${comment.commentId} class="popular_comment_btn_dislike">👎 <span class="popular_comment_dislike_count">${comment.dislikeCount}</span></button>
							            </span>
							        </div>
							        <div class="popular_comment_content">${comment.content}</div>
									<div class="popular_comments_child"></div>
							    </div>
							  `);

	// 좋아요/싫어요 클릭 이벤트
	popularCommentDiv.find(`#popular_comment_btn_like_${comment.commentId}`).off('click').on('click', function() {
		var btnId = $(this).attr("id"); // "popular_comment_btn_like_42"
		var onlyId = btnId.split("_").pop(); // 마지막 요소 = "42"
	    popularHandleReaction(onlyId, 'LIKE', $(this));
	});
	popularCommentDiv.find(`#popular_comment_btn_dislike_${comment.commentId}`).off('click').on('click', function() {
		var btnId = $(this).attr("id"); // "popular_comment_btn_like_42"
		var onlyId = btnId.split("_").pop(); // 마지막 요소 = "42"
	    popularHandleReaction(onlyId, 'DISLIKE', $(this));
	});

	return popularCommentDiv ;
}

// 댓글 아래에 대댓글 입력창 생성
$(document).on('click', '.popular_comment_btn_reply', function() {
    var commentDiv = $(this).closest('.popular_comment');
    
    // 이미 생성되어 있으면 새로 만들지 않음
    if (commentDiv.find('.child_comment_input').length > 0) return;

    var inputHtml = `
				        <div class="child_poular_comment_input" style="margin-top:5px;">
				            <textarea class="child_poular_comment_text" placeholder="답글을 500자이내로 입력하세요."></textarea>
				            <button class="child_poular_comment_submit">작성</button>
				            <button class="child_poular_comment_cancel">취소</button>
				        </div>
    				`;
    commentDiv.find('.popular_comments_child').first().prepend(inputHtml);

});

// 대댓글 입력창 취소 버튼 클릭 시 닫기
$(document).on('click', '.child_poular_comment_cancel', function() {
    $(this).closest('.child_poular_comment_input').remove();
});

// 인기 대댓글
function create_child_popular_comment() {
	// 자식댓글(대댓글 작성)
	// 자식댓글은 HTML 태그에 정적으로 선언되어있지 않고, 동적으로 script로 랜더링 되서 나중에 생성되므로,
	// (document).on('click',function()) 처리
	// 대댓글 작성 버튼 클릭
	$(document).on('click', '.child_poular_comment_submit', function() {
	    var parentDiv = $(this).closest('.popular_comment');
	    var parentCommentId = parentDiv.attr('data_popular_comment_id');
	    var content = parentDiv.find('.child_poular_comment_text').val().trim();
	    if (!content) return alert("댓글 내용을 입력하세요.");

	    ajaxWithToken({
	        url: '/comments',
	        type: 'POST',
	        contentType: 'application/json',
	        data: JSON.stringify({
	            postId: postId,
	            parentCommentId: parentCommentId,
	            content: content
	        }),
	        success: function(res) {
	            loadComments(postId, 'normal', 0); // 댓글 새로 불러오기
	        },
	        error: function(err) {
	            alert("답글 작성 중 에러가 발생했습니다.: ",err.responseText);
	        }
	    });
	});
}
// 댓

// 인기 댓글 리액션 처리
function popularHandleReaction(commentId, type, buttonElem) {
	if(!token) {
		if(confirm("로그인이 필요한 기능입니다. 로그인하시겠습니까?")) {
			window.location.href ="/signin";
		}
		return;	
	}
    ajaxWithToken({
        url: `/commentreactions/${commentId}/reaction`,
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({ commentReactionType: type }),
        success: function(res) {
            // 좋아요/싫어요 숫자 덮어쓰기
			// 버튼 클릭한 태그를 기준으로 자기자신 도는 부모중 "comment-header" 클래스를 찾고
			// 'comment-header' 태그 안에 '.comment_like_count' or '.comment_dislike_count' 찾아 .text로 좋아요, 싫어요 개수 입력
            buttonElem.closest('.popular_comment_header').find('.popular_comment_like_count').text(res.likeCount);
            buttonElem.closest('.popular_comment_header').find('.popular_comment_dislike_count').text(res.dislikeCount);

            // 내가 누른 상태 강조
            if(res.userCommentReactionType === 'LIKE') {
                buttonElem.closest('.popular_comment_header').find('.popular_comment_like_count').css('font-weight','bold');
                buttonElem.closest('.popular_comment_header').find('.popular_comment_dislike_count').css('font-weight','normal');
            } else if(res.userCommentReactionType === 'DISLIKE') {
                buttonElem.closest('.popular_comment_header').find('.popular_comment_dislike_count').css('font-weight','bold');
                buttonElem.closest('.popular_comment_header').find('.popular_comment_like_count').css('font-weight','normal');
            } else {
                buttonElem.closest('.popular_comment_header').find('.popular_comment_like_count, .popular_comment_dislike_count').css('font-weight','normal');
            }

			loadComments(postId, 'normal', 0);
        },
        error: function(xhr) {
            console.error(xhr);
            alert("리액션 처리 중 에러가 발생했습니다: " + xhr.responseText);
        }
    });
}

//*****************************************인기 댓글 처리 End******************************************************************* */

//*****************************************일반 댓글 처리 Start******************************************************************* */
// 2. 댓글 트리 렌더링
function renderComments(comments, comments_list) {
	comments_list.empty();
    comments.forEach(comment => {
        var commentElem = createCommentElem(comment);
        comments_list.append(commentElem);
    });
}

// 댓글 요소 생성 (재귀로 대댓글까지 처리)
function createCommentElem(comment) {

    // 댓글 타입 구분: 루트 댓글 vs 자식 댓글
    var typeClass = comment.parentCommentId ? " child-comment" : " root-comment";
    // pinned 여부
    var pinnedClass = comment.pinned ? " pinned" : "";

    var commentDiv = $(`
				        <div class="comment${typeClass}${pinnedClass}" data_comment_id="${comment.commentId}">
				            <div class="comment_header">
								<div class="comment_info">
									<span class="comment_hot">${comment.pinned ? '🔥' : ''}</span>
				                	<span class="comment_author">${comment.authorNickname}</span>
				                	<span class="comment_created">${comment.updatedAgo || comment.createdAt}</span>
								</div>
				                <span class="comment_actions">
				                    <button id=comment_btn_reply_${comment.commentId} class="comment_btn_reply">답글</button>
				                    <button id=comment_btn_edit_${comment.commentId} class="comment_btn_edit">수정</button>
				                    <button id=comment_btn_report_${comment.commentId} class="comment_btn_report">신고</button>
				                    <button id=comment_btn_like_${comment.commentId} class="comment_btn_like">👍 <span class="comment_like_count">${comment.likeCount}</span></button>
				                    <button id=comment_btn_dislike_${comment.commentId} class="comment_btn_dislike">👎 <span class="comment_dislike_count">${comment.dislikeCount}</span></button>
				                </span>
				            </div>
				            <div class="comment_content">${comment.content}</div>
				            <div class="comments_child"></div>
				        </div>
    				  `);

    // 대댓글 재귀
    if(comment.childComments && comment.childComments.length > 0) {
        var childContainer = commentDiv.find('.comments_child');
        comment.childComments.forEach(child => {
            childContainer.append(createCommentElem(child));
        });
    }

    // 좋아요/싫어요 클릭 이벤트
    commentDiv.find(`#comment_btn_like_${comment.commentId}`).off('click').on('click', function() {
        var btnId = $(this).attr("id");
        var onlyId = btnId.split("_").pop();
        handleReaction(onlyId, 'LIKE', $(this));
    });
    commentDiv.find(`#comment_btn_dislike_${comment.commentId}`).off('click').on('click', function() {
        var btnId = $(this).attr("id");
        var onlyId = btnId.split("_").pop();
        handleReaction(onlyId, 'DISLIKE', $(this));
    });

    return commentDiv;
}

// 댓글 아래에 대댓글 입력창 생성
$(document).on('click', '.comment_btn_reply', function() {
    var commentDiv = $(this).closest('.comment');
    
    // 이미 생성되어 있으면 새로 만들지 않음
    if (commentDiv.find('.child_comment_input').length > 0) return;

    var inputHtml = `
				        <div class="child_comment_input" style="margin-top:5px;">
				            <textarea class="child_comment_text" placeholder="답글을 500자이내로 입력하세요."></textarea>
				            <button class="child_comment_submit">작성</button>
				            <button class="child_comment_cancel">취소</button>
				        </div>
    				`;
    commentDiv.find('.comments_child').first().prepend(inputHtml);
});

// 대댓글 입력창 취소 버튼 클릭 시 닫기
// 대댓글 입력창 취소 버튼 클릭 시 닫기
$(document).on('click', '.child_comment_cancel', function() {
    // 클릭된 버튼 기준 가장 가까운 입력창 전체 제거
    $(this).closest('.child_comment_input').remove();
});


function create_child_comment() {
	// 자식댓글(대댓글 작성)
	// 자식댓글은 HTML 태그에 정적으로 선언되어있지 않고, 동적으로 script로 랜더링 되서 나중에 생성되므로,
	// (document).on('click',function()) 처리
	// 대댓글 작성 버튼 클릭
	$(document).on('click', '.child_comment_submit', function() {
	    var parentDiv = $(this).closest('.comment');
	    var parentCommentId = parentDiv.attr('data_comment_id');
	    var content = parentDiv.find('.child_comment_text').val().trim();
	    if (!content) return alert("댓글 내용을 입력하세요.");

	    ajaxWithToken({
	        url: '/comments',
	        type: 'POST',
	        contentType: 'application/json',
	        data: JSON.stringify({
	            postId: postId,
	            parentCommentId: parentCommentId,
	            content: content
	        }),
	        success: function(res) {
	            loadComments(postId, 'normal', 0); // 댓글 새로 불러오기
	        },
	        error: function(err) {
	            alert("답글 작성 중 에러가 발생했습니다.: ",err.responseText);
	        }
	    });
	});
}
// 댓글 리액션 처리
function handleReaction(commentId, type, buttonElem) {
	if(!token) {
		if(confirm("로그인이 필요한 기능입니다. 로그인하시겠습니까?")) {
			window.location.href ="/signin";
		}
		return;	
	}
    ajaxWithToken({
        url: `/commentreactions/${commentId}/reaction`,
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({ commentReactionType: type }),
        success: function(res) {
            // 좋아요/싫어요 숫자 덮어쓰기
			// 버튼 클릭한 태그를 기준으로 자기자신 도는 부모중 "comment-header" 클래스를 찾고
			// 'comment-header' 태그 안에 '.comment_like_count' or '.comment_dislike_count' 찾아 .text로 좋아요, 싫어요 개수 입력
            buttonElem.closest('.comment_header').find('.comment_like_count').text(res.likeCount);
            buttonElem.closest('.comment_header').find('.comment_dislike_count').text(res.dislikeCount);

            // 내가 누른 상태 강조
            if(res.userCommentReactionType === 'LIKE') {
                buttonElem.closest('.comment_header').find('.comment_btn_like').css('font-weight','bold');
                buttonElem.closest('.comment_header').find('.comment_btn_dislike').css('font-weight','normal');
            } else if(res.userCommentReactionType === 'DISLIKE') {
                buttonElem.closest('.comment_header').find('.comment_btn_dislike').css('font-weight','bold');
                buttonElem.closest('.comment_header').find('.comment_btn_like').css('font-weight','normal');
            } else {
                buttonElem.closest('.comment_header').find('.comment_btn_like, .comment_btn_dislike').css('font-weight','normal');
            }

			loadComments(postId, 'normal', 0);
        },
        error: function(xhr) {
            console.error(xhr);
            alert("리액션 처리 중 에러가 발생했습니다: " + xhr.responseText);
        }
    });
}

//*****************************************일반 댓글 처리 End******************************************************************* */

function commentSort() {
    $('#comment_sort').change(function() {
        var sortBy = $(this).val();
        loadComments(postId, sortBy, 0);
    });
}

// 댓글 입력 시 토큰 확인
function check_comment_login() {
	$('#new_comment_content').on('input', function() {
	    var token = localStorage.getItem('accessToken'); // 현재 액세스 토큰 가져오기
	    if (!token) {
	        if (confirm("로그인이 필요한 기능입니다. 로그인하시겠습니까?")) {
	            window.location.href = "/signin";
	        }
	        // 입력한 내용 초기화 또는 입력 막기 가능
	        $(this).val('');
	    }
	});

} 
// 부모 (최상위 댓글 작성)
function create_parent_comment() {
	// 댓글 작성 버튼 클릭 시 토큰 확인 및 댓글 생성
	$('#btn_add_comment').off('click').on('click',function() {
	    var token = localStorage.getItem('accessToken');
	    if (!token) {
	        if (confirm("로그인이 필요한 기능입니다. 로그인하시겠습니까?")) {
	            window.location.href = "/signin";
	        }
	        return; // 토큰 없으면 댓글 생성 중단
	    }

	    var content = $('#new_comment_content').val().trim();
	    if (!content) {
	        alert("댓글 내용을 입력해주세요.");
	        return;
	    }

	    // 여기서 ajax로 댓글 생성 API 호출
	    ajaxWithToken({
	        url: '/comments',
	        type: 'POST',
	        contentType: 'application/json',
	        data: JSON.stringify({ postId: postId,
								   parentCommentId: null, // 부모 댓글이므로 null 
				                   content: content }),
	        success: function(res) {
	            $('#new_comment_content').val(''); // 입력 초기화
	            loadComments(postId, 'normal', 0); // 댓글 새로 불러오기
	        },
	        error: function(err) {
	            alert("댓글 작성 중 에러가 발생했습니다. |", err.responseText);
	        }
	    });
	});
}

//*****************************************Comment API End******************************************************************
//*****************************************API End******************************************************************
$(document).ready(function() {
	/* Post API Start */
	view_count_increment(postId); //조회수 증가(그안에 게시글 불러오기)
	reaction_api(postId,token); // 게시글 리액션
	/* Post API End */

	/* Comment Start */
	commentSort(); // 댓글 최신순, 좋아요순 정렬
	loadComments(postId,"normal",0); //댓글 불러오기
	check_comment_login();
	create_parent_comment();
	create_child_popular_comment();
	create_child_comment();
	/* Comment  End */
});