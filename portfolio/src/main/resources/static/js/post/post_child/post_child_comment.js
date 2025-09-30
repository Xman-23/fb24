
// 전역 변수: 현재 페이지, 정렬 기준
var comment_currentPage = 0;
var comment_currentSort = 'normal';
var comment_totalPages = 0; // 서버에서 받은 totalPages 저장

//*****************************************Comment API Start******************************************************************
// 1. 댓글 가져오기 (통합 검색 부모, 자식 게시글에 공통으로 빼기)
function loadComments(postId, page = 0) {
    // 현재 스크롤 위치 저장
    var scrollPos = $(window).scrollTop();

    $.ajax({
        url: `/comments/post/${postId}?sortBy=${comment_currentSort}&page=${page}`,
        type: 'GET',
        success: function(response) {
				$("#popular_comments_list").empty();
				$("#comments_list").empty();
			    // 전역 변수 동기화
			    comment_totalPages = response.totalPages; // 서버 총페이지
			    comment_currentPage = page;// 현재 

			    // 댓글 수 업데이트
			    $("#post_comments").text(`${response.activeTotalElements}`);

			    // 인기 댓글 렌더링 (첫 페이지만)
			    if (page === 0) {
			        renderPopularComments(response.popularComments, $("#popular_comments_list"));
			    }

			    // 일반 댓글 렌더링
			    renderComments(response.comments, $('#comments_list'));
			    // 페이지네이션 렌더링
			    comment_renderPagination(response);

				// 댓글 로딩 후 스크롤 위치 복원
				$(window).scrollTop(scrollPos);
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

    var actionsHtml = "";
    var replyButtonHtml = "";
    var reportButtonHtml = "";
	

    // 로그인 상태일 때만 답글, 수정/삭제, 신고 버튼 처리
    if (token) {
        replyButtonHtml = `<button id="popular_comment_btn_reply_${comment.commentId}" class="popular_comment_btn_reply">답글</button>`;

        // 본인 댓글일 때만 수정/삭제 버튼 추가, 신고 버튼은 제외
        if (memberId && memberId === Number(comment.authorId)) {
            actionsHtml += `
                <button id="popular_comment_btn_edit_${comment.commentId}" class="popular_comment_btn_edit">수정</button>
                <button id="popular_comment_btn_delete_${comment.commentId}" class="popular_comment_btn_delete">삭제</button>
            `;
        } else {
            reportButtonHtml = `<button id="popular_comment_btn_report_${comment.commentId}" class="popular_comment_btn_report">신고</button>`;
        }
    }

	// 로그인 여부 상관없이 바로가기 버튼 추가
	var gotoButtonHtml = `<button id="popular_comment_btn_goto_${comment.commentId}" class="popular_comment_btn_goto">댓글로 이동</button>`;

    var popularCommentDiv = $(`
						        <div  id="comment-${comment.commentId}" class="popular_comment" data_popular_comment_id="${comment.commentId}">
						            <div class="popular_comment_header">
						                <div class="popular_comment_info">
						                    <span class="popular_comment_hot">🔥</span>
						                    <span class="popular_comment_author">${comment.authorNickname}</span>
						                    <span class="popular_comment_created">${comment.updatedAgo || comment.createdAt}</span>
						                </div>
						                <span class="popular_comment_actions">
											${gotoButtonHtml}
						                    ${replyButtonHtml}
						                    ${actionsHtml}
						                    ${reportButtonHtml}
						                    <button id="popular_comment_btn_like_${comment.commentId}" class="popular_comment_btn_like">
						                        👍 <span class="popular_comment_like_count">${comment.likeCount}</span>
						                    </button>
						                    <button id="popular_comment_btn_dislike_${comment.commentId}" class="popular_comment_btn_dislike">
						                        👎 <span class="popular_comment_dislike_count">${comment.dislikeCount}</span>
						                    </button>
						                </span>
						            </div>
						            <div class="popular_comment_content">${comment.content}</div>
						            <div class="popular_comments_child"></div>
						        </div>
    						  `);

    // 좋아요/싫어요 클릭 이벤트
    popularCommentDiv.find(`#popular_comment_btn_like_${comment.commentId}`).off('click').on('click', function() {
        var btnId = $(this).attr("id");
        var onlyId = btnId.split("_").pop();
        popularHandleReaction(onlyId, 'LIKE', $(this));
    });
    popularCommentDiv.find(`#popular_comment_btn_dislike_${comment.commentId}`).off('click').on('click', function() {
        var btnId = $(this).attr("id");
        var onlyId = btnId.split("_").pop();
        popularHandleReaction(onlyId, 'DISLIKE', $(this));
    });
	
	popularCommentDiv.find(`#popular_comment_btn_goto_${comment.commentId}`)
	                 .off('click')
	                 .on('click', function() {
	                	goToComment(comment.commentId);
	    			  });

    return popularCommentDiv;
}

function goToComment(commentId) {
    $.ajax({
		url: `/comments/${commentId}/goto-page?pageSize=10`,
        type: "GET",
		data: { sortBy: comment_currentSort },
        success: function(response) {
			console.log(response);
            const pageNumber = response.pageNumber;
            const positionInPage = response.positionInPage;

            // 페이지 이동
            goToPage(pageNumber);

            // 페이지 로딩 완료 후 스크롤
			const checkExist = setInterval(function() {
			    const targetComment = $(`.comment[data_comment_id='${commentId}']`);
			    if (targetComment.length) {
			        clearInterval(checkExist);

			        $('html, body').animate({
			            scrollTop: targetComment.offset().top - 100
			        }, 500);

			        // 강조 효과
					targetComment.css("background-color", "#fff8c4");

					setTimeout(() => {
					    targetComment.css("background-color", "");
					}, 3000);
			    }
			}, 100);
        },
        error: function(err) {
            alert(err.responseText);
        }
    });
}

// 댓글 아래에 대댓글 입력창 생성
$(document).on('click', `.popular_comment_btn_reply`, function() {
    var commentDiv = $(this).closest('.popular_comment');

	// 기존 수정창 제거
	commentDiv.find('.poular_edit_comment_form').remove();
	commentDiv.find('.popular_comment_content').show();

	var existingInput = commentDiv.find('.child_popular_comment_input');
	if (existingInput.length > 0) {
	    // 이미 있으면 제거 (취소 버튼과 동일하게 동작)
	    existingInput.remove();
	    return;
	}

    var inputHtml = `
  				      <div class="child_popular_comment_input" style="margin-top:5px;">
  				          <textarea class="child_popular_comment_text" placeholder="답글을 500자이내로 입력하세요."></textarea>
  				          <button class="child_popular_comment_submit">작성</button>
  				          <button class="child_popular_comment_cancel">취소</button>
  				      </div>
      				`;
    commentDiv.find('.popular_comments_child').first().prepend(inputHtml);

});

// 대댓글 입력창 취소 버튼 클릭 시 닫기
$(document).on('click', '.child_popular_comment_cancel', function() {
    $(this).closest('.child_popular_comment_input').remove();
});

// 인기 대댓글 생성 API
function create_child_popular_comment() {
	// 자식댓글(대댓글 작성)
	// 자식댓글은 HTML 태그에 정적으로 선언되어있지 않고, 동적으로 script로 랜더링 되서 나중에 생성되므로,
	// (document).on('click',function()) 처리
	// 대댓글 작성 버튼 클릭
	$(document).on('click', '.child_popular_comment_submit', function() {
	    var parentDiv = $(this).closest('.popular_comment');
	    var parentCommentId = parentDiv.attr('data_popular_comment_id');
	    var content = parentDiv.find('.child_popular_comment_text').val().trim();
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
	            loadComments(postId, comment_currentPage);

				// 새로 작성한 대댓글로 이동
				if (res && res.commentId) {
				    setTimeout(() => {
				        goToComment(res.commentId);
				    }, 300);
				}
	        },
	        error: function(err) {
	            alert("답글 작성 중 에러가 발생했습니다.: ",err.responseText);
	        }
	    });
	});
}

// 인기 댓글 수정 API 
$(document).on('click', `.popular_comment_btn_edit`, function() {
	// 생성된 댓글마다 가장 가까운 "popular_comment" class 찾기
    var commentDiv = $(this).closest('.popular_comment');
	// 댓글 id 가져오기
    var commentId = commentDiv.attr("data_popular_comment_id");
	// 원본 댓글 내용을 찾기위한 find
    var contentDiv = commentDiv.find(".popular_comment_content");
	// 원본 댓글 내용 가져오기
    var oldContent = contentDiv.text();

	// 기존 답글창 제거
	commentDiv.find('.child_popular_comment_input').remove();

	var existingEditForm = commentDiv.find(".poular_edit_comment_form");
	if (existingEditForm.length > 0) {
	    existingEditForm.remove();
	    contentDiv.show();
	    return;
	}

    // 수정창 생성
    var editForm = $(`
        <div class="poular_edit_comment_form">
            <textarea class="poular_edit_comment_textarea">${oldContent}</textarea>
            <button class="poular_edit_comment_save">저장</button>
            <button class="poular_edit_comment_cancel">취소</button>
        </div>
    `);

    // 기존 내용 숨기고 수정창 추가
    contentDiv.hide();
    commentDiv.append(editForm);

    // 취소 버튼 이벤트
    editForm.find(".poular_edit_comment_cancel").on("click", function() {
        editForm.remove();
        contentDiv.show();
    });
	
	editForm.find(".poular_edit_comment_save").on("click", function() {
	    var newContent = editForm.find(".poular_edit_comment_textarea").val();

	    ajaxWithToken({
	        url: `/comments/${commentId}`,
	        type: "PATCH",
	        contentType: "application/json",
	        data: JSON.stringify({ content: newContent }),
	        success: function() {
	            editForm.remove();
				loadComments(postId, comment_currentPage);
	        },
	        error: function(xhr) {
	            alert("댓글 수정 실패: " + xhr.responseText);
	        }
	    });
	});

});

// 인기 댓글 삭제 버튼 
$(document).on("click", ".popular_comment_btn_delete", function() {

	// 클릭한 태그 기준으로 가장 가까운 "popular_comment" 클래스 찾기
    var commentDiv = $(this).closest(".popular_comment");
	// 아이디 가져오기
    var commentId = commentDiv.attr("data_popular_comment_id");

    if (!confirm("정말 댓글을 삭제하시겠습니까?")) {
		return;
	}

    ajaxWithToken({
        url: `/comments/${commentId}`,  // 백엔드 DeleteMapping 엔드포인트
        type: "DELETE",
        success: function() {
            alert("댓글이 삭제되었습니다.");

			loadComments(postId, comment_currentPage);
        },
        error: function(xhr) {
            if (xhr.status === 403) {
                alert("본인 댓글만 삭제할 수 있습니다.");
            } else if (xhr.status === 404) { /*Not Found*/
                alert("댓글을 찾을 수 없습니다.");
            } else {
                alert("댓글 삭제 중 오류가 발생했습니다.");
            }
        }
    });
});

// 이벤트 바인딩 (일반, 인기 댓글 모두 사용 가능)
$(document).on("click", ".comment_btn_report, .popular_comment_btn_report", function() {
    var commentDiv = $(this).closest("[data_comment_id],[data_popular_comment_id]");
    var commentId = commentDiv.attr("data_comment_id") || commentDiv.attr("data_popular_comment_id");
    openCommentReportPopup(commentId);
});

function openCommentReportPopup(commentId) {
	
    $("#report_reason").val(""); // 초기화
    $("#comment_report_popup, #popup_overlay").show();

    $("#btn_submit_report").off('click').on('click', function() {
        var reason = $("#report_reason").val().trim();
        if(reason.length < 10) {
            alert("신고 사유는 최소 10글자 이상이어야 합니다.");
			$("#report_reason").focus();
            return;
        }

        ajaxWithToken({
            url: `/comments/${commentId}/report`,
            type: "POST",
            contentType: "application/json",
            data: JSON.stringify({ reason: reason }),
            success: function(response) {
                alert(response.message);
                $("#comment_report_popup, #popup_overlay").hide();
                loadComments(postId, comment_currentPage);
            },
            error: function(xhr) {
                alert(xhr.responseText || "신고 처리 중 오류가 발생했습니다.");
            }
        });
    });

    $("#btn_cancel_report").off('click').on('click', function() {
        $("#comment_report_popup, #popup_overlay").hide();
    });
}
// 인기 댓글 리액션 처리
function popularHandleReaction(commentId, type, buttonElem) {
	if(!token) {
		if(confirm("로그인이 필요한 기능입니다. 로그인하시겠습니까?")) {
			localStorage.setItem("redirectAfterLogin", window.location.href);
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

			loadComments(postId, comment_currentPage);
        },
        error: function(xhr) {
            console.error(xhr);
            alert(xhr.responseText);
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

function createCommentElem(comment) {

    // 댓글 타입 구분: 루트 댓글 vs 자식 댓글
    var typeClass = comment.parentCommentId ? " child-comment" : " root-comment";
    // pinned 여부
    var pinnedClass = comment.pinned ? " pinned" : "";

    // 기본 버튼 HTML
	var actionButtons = "";
	
	// 상태가 ACTIVE 일 때만 버튼 노출
	if (comment.status === "ACTIVE" && token) { // <-- token 체크 추가
	    var editButton = "";
	    var reportButton = "";

	    if (memberId && memberId === Number(comment.authorId)) {
	        // 본인 댓글이면 수정/삭제 버튼
	        editButton = `
	            <button id="comment_btn_edit_${comment.commentId}" class="comment_btn_edit">수정</button>
	            <button id="comment_btn_delete_${comment.commentId}" class="comment_btn_delete">삭제</button>
	        `;
	    } else {
	        // 타인 댓글이면 신고 버튼
	        reportButton = `<button id="comment_btn_report_${comment.commentId}" class="comment_btn_report">신고</button>`;
	    }

	    // 답글 버튼은 로그인 상태에서만
	    actionButtons = `
	        <button id="comment_btn_reply_${comment.commentId}" class="comment_btn_reply">답글</button>
	        ${editButton}
	        ${reportButton}
	    `;
	}

    var commentDiv = $(`
        <div class="comment${typeClass}${pinnedClass}" data_comment_id="${comment.commentId}">
            <div class="comment_header">
                <div class="comment_info">
                    <span class="comment_hot">${comment.pinned ? '🔥' : ''}</span>
                    <span class="comment_author">${comment.authorNickname}</span>
                    <span class="comment_created">${comment.updatedAgo || comment.createdAt}</span>
                </div>
                <span class="comment_actions">
                    ${actionButtons}
					<button id="comment_btn_like_${comment.commentId}" class="comment_btn_like">👍 
						<span class="comment_like_count">${comment.likeCount}</span>
					</button>
					<button id="comment_btn_dislike_${comment.commentId}" class="comment_btn_dislike">👎 
						<span class="comment_dislike_count">${comment.dislikeCount}</span>
					</button>
                </span>
            </div>
            <div class="comment_content">${comment.content}</div>
            <div class="comments_child"></div>
        </div>
    `);

    // 대댓글 재귀
    if (comment.childComments && comment.childComments.length > 0) {
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

	// 기존 수정창 제거
	commentDiv.children('.edit_comment_form').remove();
	commentDiv.children('.comment_content').show();

	// 이미 입력창이 존재하면 -> 취소 버튼과 동일하게 처리
	var existingInput = commentDiv.find('.child_comment_input');
	if (existingInput.length > 0) {
	    existingInput.remove(); // 입력창 제거
	    return;
	}

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
	    if (!content){
			return alert("댓글 내용을 입력하세요.");
		}

	    ajaxWithToken({
	        url: '/comments',
	        type: 'POST',
	        contentType: 'application/json',
	        data: JSON.stringify({
	            postId: postId,
	            parentCommentId: parentCommentId,
	            content: content
	        }),
	        success: function() {
	            loadComments(postId, comment_currentPage);
	        },
	        error: function(err) {
	            alert("답글 작성 중 에러가 발생했습니다.: ",err.responseText);
	        }
	    });
	});
}

// 일반 댓글 & 대댓글 수정 API 통합
$(document).on('click', '.comment_btn_edit', function() {
    var commentDiv = $(this).closest('.comment'); // 클릭한 버튼 기준
    var commentId = commentDiv.attr("data_comment_id");

    // 루트 댓글/대댓글 본문만 선택
    var contentDiv = commentDiv.children('.comment_content');
    var oldContent = contentDiv.text();

	// 기존 답글창 제거
	commentDiv.find('.child_comment_input').remove();

	// 이미 수정창이 존재하면 -> 취소 버튼과 동일하게 처리
	var existingForm = commentDiv.children(`.edit_comment_form[data_comment_id="${commentId}"]`);
	if (existingForm.length > 0) {
	    existingForm.remove();   // textarea 삭제
	    contentDiv.show();       // 원본 본문 보여주기
	    return;
	}

    // 수정창 생성
    var editForm = $(`
        <div class="edit_comment_form" data_comment_id="${commentId}">
            <textarea class="edit_comment_textarea">${oldContent}</textarea>
            <button class="edit_comment_save">저장</button>
            <button class="edit_comment_cancel">취소</button>
        </div>
    `);

    // 기존 내용 숨기고 수정창 바로 아래에 추가
    contentDiv.hide();
    editForm.insertAfter(contentDiv);

    // 취소 버튼
    editForm.find(".edit_comment_cancel").on("click", function() {
        editForm.remove();
        contentDiv.show();
    });

    // 저장 버튼
    editForm.find(".edit_comment_save").on("click", function() {
        var newContent = editForm.find(".edit_comment_textarea").val().trim();
        if (!newContent) return alert("댓글 내용을 입력하세요.");

        ajaxWithToken({
            url: `/comments/${commentId}`,
            type: "PATCH",
            contentType: "application/json",
            data: JSON.stringify({ content: newContent }),
			success: function() {
			    // 수정 후 현재 페이지 기준으로 댓글 다시 불러오기
			    editForm.remove();
			    loadComments(postId, comment_currentPage);
			},
            error: function(xhr) {
                alert("댓글 수정 실패: " + xhr.responseText);
            }
        });
    });
});

// 일반 댓글 삭제 버튼 이벤트
$(document).on("click", ".comment_btn_delete", function() {
    // 클릭한 버튼 기준으로 가장 가까운 댓글 div 찾기
    var commentDiv = $(this).closest(".comment");
    var commentId = commentDiv.attr("data_comment_id");

    if (!confirm("정말 댓글을 삭제하시겠습니까?")) {
        return;
    }

    ajaxWithToken({
        url: `/comments/${commentId}`, // 백엔드 DeleteMapping 엔드포인트
        type: "DELETE",
        success: function() {
            alert("댓글이 삭제되었습니다.");
            // 삭제 후 댓글 목록 새로고침
            loadComments(postId, comment_currentPage);
        },
        error: function(xhr) {
            if (xhr.status === 403) {
                alert("본인 댓글만 삭제할 수 있습니다.");
            } else if (xhr.status === 404) {
                alert("댓글을 찾을 수 없습니다.");
            } else {
                alert("댓글 삭제 중 오류가 발생했습니다.");
            }
        }
    });
});


// 댓글 리액션 처리
function handleReaction(commentId, type, buttonElem) {
	if(!token) {
		if(confirm("로그인이 필요한 기능입니다. 로그인하시겠습니까?")) {
			localStorage.setItem("redirectAfterLogin", window.location.href);
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

			loadComments(postId, comment_currentPage);
        },
        error: function(xhr) {
            console.error(xhr);
            alert(xhr.responseText);
        }
    });
}

//*****************************************일반 댓글 처리 End******************************************************************* */

//*****************************************부모 댓글 작성 Start******************************************************************* */
// 댓글 입력 시 토큰 확인
function check_comment_login() {
	
	// 공통 selector: 부모, 인기 대댓글, 인기 수정, 일반 대댓글, 일반 수정
	const commentTextareas = `
	    #new_comment_content,
	    .child_popular_comment_text,
	    .poular_edit_comment_textarea,
	    .child_comment_text,
	    .edit_comment_textarea
	`;

	$(document).on('input', commentTextareas, function() {
	    var token = localStorage.getItem('accessToken');
	    if (!token) {
	        if (confirm("로그인이 필요한 기능입니다. 로그인하시겠습니까?")) {
				localStorage.setItem("redirectAfterLogin", window.location.href);
	            window.location.href = "/signin";
	        }
	        $(this).val(''); // 입력 초기화
	    }
	});
}

// 부모 (최상위 댓글 작성)
function create_parent_comment() {
    $('#btn_add_comment').off('click').on('click', function() {
        var token = localStorage.getItem('accessToken');
        if (!token) {
            if (confirm("로그인이 필요한 기능입니다. 로그인하시겠습니까?")) {
                localStorage.setItem("redirectAfterLogin", window.location.href);
                window.location.href = "/signin";
            }
            return;
        }

        var content = $('#new_comment_content').val().trim();
        if (!content) {
            alert("댓글 내용을 입력해주세요.");
            return;
        }

        ajaxWithToken({
            url: '/comments',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ postId: postId, parentCommentId: null, content: content }),
            success: function(res) {
                $('#new_comment_content').val('');

				if (res && res.commentId) {
				    setTimeout(() => {
				        goToComment(res.commentId);
				    }, 300);
				}
            },
            error: function(err) {
                alert("댓글 작성 중 에러가 발생했습니다. | " + err.responseText);
            }
        });
    });
}
//*****************************************부모 댓글 작성 End******************************************************************* */

//***************************************** 댓글 페이징 처리 Start ******************************************************************* */
// 페이지 이동 + 댓글 로딩 공통 함수
function goToPage(page) {
    // 범위 체크
    if (page < 0) {
		page = 0;
	} 
	// 마지막 페이지 작업
    if (comment_totalPages && page >= comment_totalPages) {
		page = comment_totalPages - 1;
	}

	// 현재 페이지 작업
    comment_currentPage = page;
    loadComments(postId,comment_currentPage);
}

// 댓글 페이지네이션 렌더링 
function comment_renderPagination(data) {
    var currentPage = data.pageNumber; // 현재 페이지 index
    var totalPages = data.totalPages;  // 총 페이지 수
    var lastPage = totalPages - 1;

    let pageButtonsContainer = $("#comment_page_buttons");
    pageButtonsContainer.empty();

    const maxPageButtons = 5;
    const group = Math.floor(currentPage / maxPageButtons);
    const startPage = group * maxPageButtons;
    const endPage = Math.min(startPage + maxPageButtons - 1, lastPage);

    // 숫자 버튼 생성
    for (let i = startPage; i <= endPage; i++) {
        let btn = $(`<button class="page_btn">${i + 1}</button>`);
        if (i === currentPage) btn.prop("disabled", true); // 현재 페이지 비활성화
		// 2,3,4,5,6,7... 페이지 클릭시
        btn.click(() => {
            goToPage(i);
        });
        pageButtonsContainer.append(btn);
    }

    // 이전 버튼
    $("#comment_prev_page").prop("disabled", !data.hasPrevious)
				           .off("click")
				           .click(() => {
				            	if (data.hasPrevious) goToPage(Math.max(currentPage - 1, 0));
				        	});

    // 다음 버튼
    $("#comment_next_page").prop("disabled", !data.hasNext)
					       .off("click")
					       .click(() => {
					            	if (data.hasNext) goToPage(Math.min(currentPage + 1, lastPage));
					       });

    // 처음 버튼
    $("#comment_first_page").prop("disabled", !data.hasFirst)
                            .off("click")
                            .click(() => {
            						if (data.hasFirst) goToPage(0);
        					});

    // 마지막 버튼
    $("#comment_last_page").prop("disabled", !data.hasLast)
                           .off("click")
                           .click(() => {
             						if (data.hasLast) goToPage(lastPage);
        					});
}
//***************************************** 댓글 페이징 처리 End ******************************************************************* */

// 댓글 정렬 함수
function change_comment_sort() {
	// 정렬 변경
	$('#comment_sort').off('change').on('change', function() {
		// 셀렉트 박스 value값을 전역변수인 "comment_currentSort" setting
	    comment_currentSort = $(this).val();
		// 정렬할시 첫페이지로 이동
	    goToPage(0);
	});
}

$(document).ready(function() {
	
	const hash = window.location.hash;
	if (hash.startsWith("#comment-")) {
	    const commentId = hash.replace("#comment-", "");
	    if (commentId) {
	        goToComment(commentId);
	    }
	}

	// 페이지 초기화 Start
	goToPage(0);
	change_comment_sort();
	// 페이지 초기화 End
	/* Comment Start */
	check_comment_login(); //댓글 입력시 토큰으로 로그인 상태 황인
	create_parent_comment(); // 부모 댓글 작성
	create_child_popular_comment();  // 인기 댓글 대댓글 작성
	create_child_comment(); // 일반댓글 대댓글 작성
	/* Comment  End */
});