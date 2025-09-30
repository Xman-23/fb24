//*************************************************** 전역변수 Start ***************************************************/

var memberInfo;

//*************************************************** 전역변수 End ***************************************************/
//*************************************************** Function  Start ***************************************************/

function logoutAndRedirect() {
    // 토큰 삭제 등 로그아웃 처리
    sessionStorage.removeItem("accessToken");
    sessionStorage.removeItem("refreshToken");
    window.location.href = "/"; // 메인 페이지 이동
}

// 관리자 전용 섹션 표시
function showAdminSection() {
    var role = localStorage.getItem('role'); // ROLE_USER, ROLE_ADMIN
    if (role === 'ROLE_ADMIN') {
    	$('#adminSectionSpan').text(role);
        $('#adminSection').show();
    } else {
        $('#adminSection').hide(); // 혹시 안전하게
    }
}

// 탭 활성화 처리 함수
function activateTab(tabId) {
    // 버튼 active 처리
    $('.tab-btn').removeClass('active');
    $(`.tab-btn[data-tab="${tabId}"]`).addClass('active');

    // 탭 컨텐츠 표시
    $('.tab-content').removeClass('active').hide();
    $('#' + tabId).addClass('active').show();

	if (tabId === 'info-tab') {
	    load_member_info_api();
	} else if (tabId === 'my-posts-tab') {
	    load_my_posts_api(0);
	} else if (tabId === 'comments-tab') {
		load_my_comments_api(0);  // 댓글용 API 호출
	}
}

$(document).on("click", "#createBoardBtn", function() {
    window.location.href = "/board_parent_create"; // 부모게시판 생성
});

//*************************************************** Function End***************************************************/

//*************************************************** API START ***************************************************//

// 기존 회원정보 조회
function load_member_info_api() {
    ajaxWithToken({
        url: '/members/me',
        type: 'GET',
        success: function(res) {
			memberInfo = res;
            $('#username').text(res.username);
            $('#nickname').text(res.nickname);
            $('#phoneNumber').text(res.phoneNumber);
            $('#address').text(res.address);
            $('#memberGradeLevel').text(res.memberGradeLevel);
            // 게시글 알림 색상입히기 (활성(notification-active) : 파란색), (비활성(notification-inactive) : 빨간색) 
            $('#postNotification')
            	.text(res.postNotificationEnabled ? '활성' : '비활성')
            	.removeClass('notification-active notification-inactive')
            	.addClass(res.postNotificationEnabled ? 'notification-active' : 'notification-inactive');
            $('#commentNotification')
            	.text(res.commentNotificationEnabled ? '활성' : '비활성')
            	.removeClass('notification-active notification-inactive')
            	.addClass(res.commentNotificationEnabled ? 'notification-active' : 'notification-inactive');
            showAdminSection();
        },
        error: function(xhr) {
            $('#member-info').html(`<p style="color:red;">${xhr.responseText || '회원정보를 가져오는데 실패했습니다.'}</p>`);
        }
    });
}

function update_info() {
	$("#update_info_btn").off('click').on('click',function () {
        	sessionStorage.setItem("editMemberPrefill",  JSON.stringify(memberInfo));
        	window.location.href = '/member_me_update';
	})
}

function member_withdraw_api() {

    $("#withdraw-btn").off("click").on("click", function() {
        if (!confirm("정말 탈퇴하시겠습니까? 탈퇴 후 복구가 불가능합니다.")) {
            return;
        }

        // AJAX PATCH 요청
        ajaxWithToken({
            url: "/members/me/withdraw",
            type: "PATCH",
            success: function(res) {
                alert(res); // "회원 탈퇴가 정상 처리되었습니다."
                // 로그아웃 처리 후 메인 페이지 이동
                logoutAndRedirect();
            },
            error: function(xhr) {
                alert(xhr.responseText || "회원 탈퇴에 실패했습니다.");
            }
        });
    });
}


// 내 게시글 보기
function load_my_posts_api(page = 0) {
    ajaxWithToken({
        url: `/posts/me?page=${page}`,  // size, sort 필요 없음
        type: "GET",
        success: function(res) {
            const tbody = $("#my-posts-table-tbody");
            tbody.empty();

            if (!res.normalPosts || res.normalPosts.length === 0) {
                tbody.append(`<tr><td colspan="6">작성한 게시글이 없습니다.</td></tr>`);
                return;
            }

			res.normalPosts.forEach(post => {
			    // 링크 분기
			    let postUrl = post.boardId === 1 ? `/board/${post.boardId}/notice/${post.postId}` 
				                                 : `/board/${post.boardId}/normal/${post.postId}`;

			    const row = `
			        <tr>
			            <td>${post.no}</td>
			            <td style="text-align:left;">
			                <a href="${postUrl}" class="post-link">${post.title}</a>
			            </td>
			            <td>${post.boardName}</td>
			            <td>${post.createdAt}</td>
						<td>${post.reactionCount}
			            <td>${post.viewCount}</td>
			        </tr>
			    `;
			    tbody.append(row);
			});

			// 게시글 페이징
			render_pagination(res, 'my-posts', load_my_posts_api);
        },
        error: function(xhr) {
            alert(xhr.responseText || "내 게시글을 불러오지 못했습니다.");
        }
    });
}

// 내 댓글보기
function load_my_comments_api(page = 0) {
    ajaxWithToken({
        url: `/comments/me?page=${page}`,
        type: "GET",
        success: function(res) {

            const tbody = $("#my-comments-table-tbody");
            tbody.empty();

            if (!res.comments || res.comments.length === 0) {
                tbody.append(`<tr><td colspan="6">작성한 댓글이 없습니다.</td></tr>`);
                return;
            }

            res.comments.forEach(comment => {
				let commentUrl = comment.boardId === 1 ? `/board/${comment.boardId}/notice/${comment.postId}` 
				                                       : `/board/${comment.boardId}/normal/${comment.postId}`;

                const row = `
                    <tr>
                        <td>${comment.no}</td>
						<td style="text-align:left;">
						    <a href="${commentUrl}#comment-${comment.commentId}" 
						       class="comment-link" 
						       data-comment-id="${comment.commentId}">
						       ${comment.content}
						    </a>
						</td>
						<td>${comment.boardName}</td>
                        <td>${comment.createdAt}</td>
						<td>${comment.likeCount}</td>
                    </tr>
                `;
                tbody.append(row);
            });

			// 댓글 페이징
			render_pagination(res, 'my-comments', load_my_comments_api);
        },
        error: function(xhr) {
            alert(xhr.responseText || "내 댓글을 불러오지 못했습니다.");
        }
    });
}

//*************************************************** API START ***************************************************//

//*************************************************** 내정보 페이징 처리 START ***************************************************//
/*
// 게시글 페이징
function render_my_posts_pagination(data) {
    var currentPage = data.pageNumber; // 현재 페이지 index
    var totalPages = data.totalPages;  // 총 페이지 수
    var lastPage = totalPages - 1;

    const pageButtonsContainer = $("#my-posts-page-buttons");
    pageButtonsContainer.empty();

    const maxPageButtons = 10;
    const group = Math.floor(currentPage / maxPageButtons);
    const startPage = group * maxPageButtons;
    const endPage = Math.min(startPage + maxPageButtons - 1, lastPage);

    // 내 게시글 API 호출
    function loadPage(targetPage) {
        load_my_posts_api(targetPage);
    }

    // 1) 페이지 번호 버튼
    for (let i = startPage; i <= endPage; i++) {
        let btn = $(`<button>${i + 1}</button>`);
        if (i === currentPage) {
            btn.prop("disabled", true);
        }
        btn.click(() => loadPage(i));
        pageButtonsContainer.append(btn);
    }

    // 2) 이전 버튼
    $("#my-posts-prev-page")
        .prop("disabled", !data.hasPrevious)
        .off("click")
        .click(() => {
            if (data.hasPrevious) {
                loadPage(Math.max(currentPage - 1, 0));
            }
        });

    // 3) 다음 버튼
    $("#my-posts-next-page")
        .prop("disabled", !data.hasNext)
        .off("click")
        .click(() => {
            if (data.hasNext) {
                loadPage(Math.min(currentPage + 1, lastPage));
            }
        });

    // 4) 처음 버튼
    $("#my-posts-first-page")
        .prop("disabled", !data.hasFirst)
        .off("click")
        .click(() => {
            if (data.hasFirst) {
                loadPage(0);
            }
        });

    // 5) 마지막 버튼
    $("#my-posts-last-page")
        .prop("disabled", !data.hasLast)
        .off("click")
        .click(() => {
            if (data.hasLast) {
                loadPage(lastPage);
            }
        });

    // 6) 점프 뒤로
    $("#my-posts-jump-backward")
        .prop("disabled", data.jumpBackwardPage == null || data.jumpBackwardPage === currentPage)
        .off("click")
        .click(() => {
            if (data.jumpBackwardPage != null && data.jumpBackwardPage !== currentPage) {
                loadPage(data.jumpBackwardPage);
            }
        });

    // 7) 점프 앞으로
    $("#my-posts-jump-forward")
        .prop("disabled", data.jumpForwardPage == null || data.jumpForwardPage === currentPage)
        .off("click")
        .click(() => {
            if (data.jumpForwardPage != null && data.jumpForwardPage !== currentPage) {
                loadPage(data.jumpForwardPage);
            }
        });
}

// 댓글 페이징
function render_my_comments_pagination(data) {
    var currentPage = data.pageNumber;
    var totalPages = data.totalPages;
    var lastPage = totalPages - 1;

    const pageButtonsContainer = $("#my-comments-page-buttons");
    pageButtonsContainer.empty();

    const maxPageButtons = 10;
    const group = Math.floor(currentPage / maxPageButtons);
    const startPage = group * maxPageButtons;
    const endPage = Math.min(startPage + maxPageButtons - 1, lastPage);

    // 내 댓글 API 호출
    function loadPage(targetPage) {
        load_my_comments_api(targetPage);
    }

    // 1) 페이지 번호 버튼
    for (let i = startPage; i <= endPage; i++) {
        let btn = $(`<button>${i + 1}</button>`);
        if (i === currentPage) {
            btn.prop("disabled", true);
        }
        btn.click(() => loadPage(i));
        pageButtonsContainer.append(btn);
    }

    // 2) 이전 버튼
    $("#my-comments-prev-page")
        .prop("disabled", !data.hasPrevious)
        .off("click")
        .click(() => {
            if (data.hasPrevious) {
                loadPage(Math.max(currentPage - 1, 0));
            }
        });

    // 3) 다음 버튼
    $("#my-comments-next-page")
        .prop("disabled", !data.hasNext)
        .off("click")
        .click(() => {
            if (data.hasNext) {
                loadPage(Math.min(currentPage + 1, lastPage));
            }
        });

    // 4) 처음 버튼
    $("#my-comments-first-page")
        .prop("disabled", !data.hasFirst)
        .off("click")
        .click(() => {
            if (data.hasFirst) {
                loadPage(0);
            }
        });

    // 5) 마지막 버튼
    $("#my-comments-last-page")
        .prop("disabled", !data.hasLast)
        .off("click")
        .click(() => {
            if (data.hasLast) {
                loadPage(lastPage);
            }
        });

    // 6) 점프 뒤로
    $("#my-comments-jump-backward")
        .prop("disabled", data.jumpBackwardPage == null || data.jumpBackwardPage === currentPage)
        .off("click")
        .click(() => {
            if (data.jumpBackwardPage != null && data.jumpBackwardPage !== currentPage) {
                loadPage(data.jumpBackwardPage);
            }
        });

    // 7) 점프 앞으로
    $("#my-comments-jump-forward")
        .prop("disabled", data.jumpForwardPage == null || data.jumpForwardPage === currentPage)
        .off("click")
        .click(() => {
            if (data.jumpForwardPage != null && data.jumpForwardPage !== currentPage) {
                loadPage(data.jumpForwardPage);
            }
        });
}
*/

function render_pagination(data, prefix, loadApiCallback) {
    var currentPage = data.pageNumber;
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

//*************************************************** 내정보 페이징 처리 START ***************************************************//

$(document).ready(function() {
	// 탭 버튼 클릭
	$('.tab-btn').on('click', function() {
	    var tabId = $(this).data('tab');
	    activateTab(tabId);
	});

	// 초기 활성화 탭 설정
	activateTab('info-tab');
	// 회원정보 변경 페이지 이동
	update_info();

	// 회원탈퇴 API
	member_withdraw_api();
});