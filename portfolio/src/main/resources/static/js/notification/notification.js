//*************************************************** 전역변수 Start ***************************************************/
var notificationMemberInfo;

//*************************************************** 전역변수 End ***************************************************/

//*************************************************** Helper Method Start ***************************************************/
function notification_post_render(res) {
    res.notifications.forEach(post => {
        let postUrl = "#"; // 기본값
        let readText = post.read ? "읽음" : "안 읽음";
        let rowClass = post.read ? "read-row" : "";

        if (post.notificationType === "POST_WARNED_DELETED") {
            readText = "경고 삭제";
            rowClass = "warned-row"; // 빨간색 행 표시
        } else {
            // 게시글 댓글/ 좋아요 알림 구분
            if (post.notificationType === "POST_COMMENT") {
                postUrl = post.boardId === 1 ? `/board/${post.boardId}/notice/${post.postId}#comment-${post.commentId}`
                    						 : `/board/${post.boardId}/normal/${post.postId}#comment-${post.commentId}`;
            } else {
                postUrl = post.boardId === 1 ? `/board/${post.boardId}/notice/${post.postId}`
                    						 : `/board/${post.boardId}/normal/${post.postId}`;
            }
        }

        let title = post.senderNickname + post.notificationMessage;

        const row = `
            <tr class="${rowClass}">
                <td style="text-align:left;">
                    ${post.notificationType === "POST_WARNED_DELETED" ? `<span>${title}</span>` 
                        											  : `<a href="${postUrl}" class="notification-link" data-notification-id="${post.notificationId}">${title}</a>`
                    }
                </td>
                <td>${readText}</td>
                <td>
                    <button id="post-delete-notification-btn-${post.notificationId}" class="post-delete-notification-btn" data-id="${post.notificationId}">삭제</button>
                </td>
            </tr>
        `;
        $("#my-posts-notification-table-tbody").append(row);
    });
}

function notification_comment_render(res) {
	
	res.notifications.forEach(comment => {
	    let commentUrl = "#"; // 기본값
	    let readText = comment.read ? "읽음" : "안 읽음";
	    let rowClass = comment.read ? "read-row" : "";

	    if (comment.notificationType === "COMMENT_WARNED_DELETED") {
	        readText = "경고 삭제";
	        rowClass = "warned-row"; // 빨간색 행 표시
	    } else {
	        // 대댓글 알림 구분 / 댓글 좋아요 구분
            commentUrl = comment.boardId === 1 ? `/board/${comment.boardId}/notice/${comment.postId}#comment-${comment.commentId}`
                						       : `/board/${comment.boardId}/normal/${comment.postId}#comment-${comment.commentId}`;
	    }

	    let title = comment.senderNickname + comment.notificationMessage;

	    const row = `
	        <tr class="${rowClass}">
	            <td style="text-align:left;">
	                ${comment.notificationType === "POST_WARNED_DELETED" ? `<span>${title}</span>` 
	                    												 : `<a href="${commentUrl}" class="notification-link" data-notification-id="${comment.notificationId}">${title}</a>`
	                }
	            </td>
	            <td>${readText}</td>
	            <td>
	                <button id="comment-delete-notification-btn-${comment.notificationId}" class="comment-delete-notification-btn" data-id="${comment.notificationId}">삭제</button>
	            </td>
	        </tr>
	    `;
	    $("#my-comments-notification-table-tbody").append(row);
	});
}
//*************************************************** Helper Method End ***************************************************/

//*************************************************** API Start ***************************************************/

// 전체 알림 읽음 처리
$(document).on("click", "#mark-all-read", function() {
    if (!confirm("notification_hader_mark_all_read")) {
		return;
	}

    ajaxWithToken({
        url: "/notifications/read/all",
        type: "PATCH",
        success: function() {
            alert("모든 알림이 읽음 처리되었습니다.");
			location.reload(); // 페이지 새로고침
        },
        error: function(xhr) {
            alert(xhr.responseText || "모든 알림 읽음 처리에 실패했습니다.");
        }
    });
});

// 전체 알림 삭제 (논리적 삭제)
$(document).on("click", "#notification_hader_delete_all_button", function() {
    if (!confirm("모든 알림을 삭제하시겠습니까?")) {
		return;
	}

    ajaxWithToken({
        url: "/notifications/delete/all",
        type: "DELETE",
        success: function() {
            alert("모든 알림이 삭제되었습니다.");
			location.reload(); // 페이지 새로고침
        },
        error: function(xhr) {
            alert(xhr.responseText || "모든 알림 삭제에 실패했습니다.");
        }
    });
});

function notification_init(initialTabId) {
	
	// 토큰 있으면 회원 정보 가져오기
	ajaxWithToken({
	    url: '/members/me',
	    type: 'GET',
	    success: function(res) {
			notificationMemberInfo = res;
	        notification_total_init(memberInfo);
			
			// 초기 활성화 탭 설정
			activateTab(initialTabId);
	    },
	    error: function(xhr) {
	        alert.error(xhr.responseText);
	        // 인증 실패 시 토큰 제거 후 다시 로그인/회원가입 버튼 표시
	        localStorage.removeItem('accessToken');
			localStorage.removeItem("refreshToken");
	    }
	});

}

function notification_total_init(memberInfo) {

	$("#page-posts-unread-count").empty();
	$("#page-posts-total-count").empty();
	$("#page-comments-unread-count").empty();
	$("#page-comments-total-count").empty();

	// 게시글 알림 사용 여부 체크
	if (!memberInfo.postNotificationEnabled) {
		$("#my-posts-notification-table-tbody").empty();
	    $("#my-posts-notification-table-tbody").append(`
	        <tr>
				<td colspan="3">
					<button id="go-post-settings">알림 설정하러 가기</button>
				</td>
			</tr>
	    `);
	    $("#page-posts-unread-count").text(0);
	    $("#page-posts-total-count").text(0);
	} else {
	    loadPostNotificationsCount();
	}

	// 댓글 알림 사용 여부 체크
	if (!memberInfo.commentNotificationEnabled) {
		$("#my-comments-notification-table-tbody").empty();
	    $("#my-comments-notification-table-tbody").append(`
			<tr>
				<td colspan="3">
					<button id="go-comment-settings">알림 설정하러 가기</button>
				</td>
			</tr>
	    `);
	    $("#page-comments-unread-count").text(0);
	    $("#page-comments-total-count").text(0);
	} else {
	    loadCommentNotificationsCount();
	}
	//
	updateDeleteAllButtonState();
}

// 게시글 알림 갯수
function loadPostNotificationsCount() {
	var postCountUrl = "/notifications/count/posts/unread";
	
	
	ajaxWithToken({
	    url: postCountUrl,
	    type: "GET",
	    success: function(postunReadCount) {
			// 게시글 알림이 100개 이상이면 "100+"로 표시
			if (postunReadCount > 1000) {
			    $("#page-posts-unread-count").text("1000+");
			} else {
			    $("#page-posts-unread-count").text(postunReadCount);
			}

			ajaxWithToken({
			    url: "/notifications/count/posts",
			    type: "GET",
			    success: function(postTotal) {
					if (postTotal > 1000) {
					    $("#page-posts-total-count").text("1000+");
					} else {
					    $("#page-posts-total-count").text(postTotal);
					}
			    },
				error: function() {
				    console.error("게시글 읽은 알림 개수 조회 실패");
				}
			});
	    },
	    error: function() {
	        console.error("게시글 안 읽은 알림 개수 조회 실패");
	    }
	});
}

function loadCommentNotificationsCount() {
	var commentCountUrl = "/notifications/count/comments/unread";
	
	ajaxWithToken({
	    url: commentCountUrl,
	    type: "GET",
	    success: function(commentunReadCount) {
			// 게시글 알림이 100개 이상이면 "100+"로 표시
			if (commentunReadCount > 1000) {
			    $("#page-comments-unread-count").text("1000+");
			} else {
			    $("#page-comments-unread-count").text(commentunReadCount);
			}
			
			
			ajaxWithToken({
			    url: "/notifications/count/comments",
			    type: "GET",
			    success: function(commentTotal) {
					// 게시글 알림이 100개 이상이면 "100+"로 표시
					if (commentTotal > 1000) {
					    $("#page-comments-total-count").text("1000+");
					} else {
					    $("#page-comments-total-count").text(commentTotal);
					}
			    },
				error: function() {
				    console.error("댓글 읽은 알림 개수 조회 실패");
				}
			});
	    },
	    error: function() {
	        console.error("댓글 안 읽은 알림 개수 조회 실패");
	    }
	});
}

// 모두 읽기, 모두 삭제 활성화 여부 
function updateDeleteAllButtonState() {
	
}

// 탭 활성화 처리 함수
function activateTab(tabId) {
    // 버튼 active 처리
	// 탭 바꿀시 'active'클래스 지운 후,
    $('.tab-btn').removeClass('active');
	// 'post-notification-tab' 또는 'comment-notification-ta'에 'active' 클래스 추가
    $(`.tab-btn[data-tab="${tabId}"]`).addClass('active');

	// 탭 컨텐츠 표시
	// 탭 바꿀시 'active'클래스 지운 후 감추기
	$('.tab-content').removeClass('active').hide();
	// 감춘후에, 'post-notification-tab' 또는 'comment-notification-ta'에 'active' 클래스 추가 후 보여주기
	$('#' + tabId).addClass('active').show();

	if (tabId === 'post-notification-tab') {
		// 알림 설정했을때만 알림 보여주기
	    if (notificationMemberInfo && notificationMemberInfo.postNotificationEnabled) {
	        load_my_posts_notification_api(0);
	    }
	} else if (tabId === 'comment-notification-tab') {
		// 알림 설정했을때만 알림 보여주기
	    if (notificationMemberInfo && notificationMemberInfo.commentNotificationEnabled) {
	        load_my_comments_notification_api(0);
	    }
	}
}

// 내 게시글 보기
function load_my_posts_notification_api(page=0) {
    ajaxWithToken({
        url: `/notifications/posts?page=${page}`, 
        type: "GET",
        success: function(res) {


            $("#my-posts-notification-table-tbody").empty();

			if (!res.notifications || res.notifications.length === 0) {
			    $("#my-posts-notification-table-tbody").append(`
			        <tr>
			            <td colspan="3" class="no-data">게시글 알림이 없습니다.</td>
			        </tr>
			    `);
			    return;
			}else {
				notification_post_render(res);
			}

            // 게시글 페이징
            render_notification_pagination(res, 'my-posts-notification', load_my_posts_notification_api);
        },
        error: function(xhr) {
            alert(xhr.responseText || "내 게시글을 불러오지 못했습니다.");
        }
    });
}

// 내 댓글 보기
function load_my_comments_notification_api(page=0) {
    ajaxWithToken({
        url: `/notifications/comments?page=${page}`, 
        type: "GET",
        success: function(res) {

            $("#my-comments-notification-table-tbody").empty();

			if (!res.notifications || res.notifications.length === 0) {
			    $("#my-comments-notification-table-tbody").append(`
			        <tr>
			            <td colspan="3" class="no-data">댓글 알림이 없습니다.</td>
			        </tr>
			    `);
			    return;
			}else {
				notification_comment_render(res);
			}

            // 게시글 페이징
            render_notification_pagination(res, 'my-comments-notification', load_my_comments_notification_api);
        },
        error: function(xhr) {
            alert(xhr.responseText || "내 게시글을 불러오지 못했습니다.");
        }
    });
}

// 알림 단건 삭제 이벤트
$(document).on("click", ".post-delete-notification-btn, .comment-delete-notification-btn", handleNotificationDelete);

function handleNotificationDelete() {
    let notificationId = $(this).data("id");

    if (!confirm("이 알림을 삭제하시겠습니까?")) {
		return;
	}

    ajaxWithToken({
        url: `/notifications/${notificationId}`,
        type: "DELETE",
        success: function() {
            alert("알림이 삭제되었습니다.");

			// 삭제 후 알림 카운트 새로 갱신
			notification_total_init(notificationMemberInfo);

            // 현재 페이지 번호 구하기
            let currentPage = 0;
            let disabledBtn;

            if ($(this).hasClass("post-delete-notification-btn")) {
                disabledBtn = $("#my-posts-notification-page-buttons button:disabled");
                currentPage = disabledBtn.length ? Number(disabledBtn.text()) - 1 : 0;

                load_my_posts_notification_api(currentPage);

            } else if ($(this).hasClass("comment-delete-notification-btn")) {
                disabledBtn = $("#my-comments-notification-page-buttons button:disabled");
                currentPage = disabledBtn.length ? Number(disabledBtn.text()) - 1 : 0;

                load_my_comments_notification_api(currentPage);
            }
        }.bind(this), // success() '안'에서는 'this'가 태그를 참조하지 않음,
		              // 그러므로 bind(this)를 사용하여 'this'가 태그를 참조하게 해야함
        error: function(xhr) {
            alert(xhr.responseText || "알림 삭제에 실패했습니다.");
        }
    });
}

function render_notification_pagination(data, prefix, loadApiCallback) {
    var currentPage = Number(data.pageNumber);
    var totalPages = data.totalPages;
    var lastPage = totalPages - 1;

    const pageButtonsContainer = $(`#${prefix}-page-buttons`);
    pageButtonsContainer.empty();

    const maxPageButtons = 10;
    const group = Math.floor(currentPage / maxPageButtons);
    const startPage = group * maxPageButtons;
    const endPage = Math.min(startPage + maxPageButtons - 1, lastPage);

    function loadPage(targetPage) {
        loadApiCallback(targetPage);
    }

    // 페이지 번호 버튼
    for (let i = startPage; i <= endPage; i++) {
        let btn = $(`<button>${i + 1}</button>`);
		if (i === currentPage) {
		    btn.prop("disabled", true).attr("disabled", "disabled").addClass("active-page");
		}
        btn.click(() => loadPage(i));
        pageButtonsContainer.append(btn);
    }

    // 이전 버튼
    $(`#${prefix}-prev-page`)
        .prop("disabled", !data.hasPrevious)
        .off("click")
        .click(() => { if(data.hasPrevious) loadPage(Math.max(currentPage - 1, 0)); });

    // 다음 버튼
    $(`#${prefix}-next-page`)
        .prop("disabled", !data.hasNext)
        .off("click")
        .click(() => { if(data.hasNext) loadPage(Math.min(currentPage + 1, lastPage)); });

    // 처음 버튼
    $(`#${prefix}-first-page`)
        .prop("disabled", !data.hasFirst)
        .off("click")
        .click(() => { if(data.hasFirst) loadPage(0); });

    // 마지막 버튼
    $(`#${prefix}-last-page`)
        .prop("disabled", !data.hasLast)
        .off("click")
        .click(() => { if(data.hasLast) loadPage(lastPage); });

    // 점프 뒤로
    $(`#${prefix}-jump-backward`)
        .prop("disabled", data.jumpBackwardPage == null || data.jumpBackwardPage === currentPage)
        .off("click")
        .click(() => { if(data.jumpBackwardPage != null && data.jumpBackwardPage !== currentPage) loadPage(data.jumpBackwardPage); });

    // 점프 앞으로
    $(`#${prefix}-jump-forward`)
        .prop("disabled", data.jumpForwardPage == null || data.jumpForwardPage === currentPage)
        .off("click")
        .click(() => { if(data.jumpForwardPage != null && data.jumpForwardPage !== currentPage) loadPage(data.jumpForwardPage); });
}

//*************************************************** API End ***************************************************/

$(document).ready(function() {
    // URLSearchParams 로 쿼리스트링 파싱
    const params = new URLSearchParams(window.location.search);
    let tabParam = params.get('tab'); // post-notification, comment-notification 둘 중 하나

    // tabParam을 실제 data-tab 값으로 변환
    let initialTabId = 'post-notification-tab'; // 기본값
    if (tabParam === 'post-notification') {
        initialTabId = 'post-notification-tab';
    } else if (tabParam === 'comment-notification') {
        initialTabId = 'comment-notification-tab';
    }

	// 페이지 정보 읽기 (기본값은 0)
	let initialPage = parseInt(params.get('page')) || 0;
	
    // 초기화
    notification_init(initialTabId);

    // 탭 버튼 클릭 이벤트
    $('.tab-btn').on('click', function() {
        var tabId = $(this).data('tab');
        activateTab(tabId);
    });
});