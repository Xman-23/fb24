// ===================== 전역 변수 =====================
var refreshTimer = null;        // setTimeout 중복 방지용 타이머 변수
var refreshRetryCount = 0;      // 자동 갱신 재시도 카운트
var MAX_RETRY = 3;              // 토큰 갱신 최대 재시도 횟수
var RETRY_INTERVAL = 5000;      // 실패 시 재시도 간격 (밀리초 단위)
var ajaxRetryCountMap = {};     // AJAX 요청별 401 재시도 카운트 저장

var memberInfo;

// 총 카운트
var totalCounts = { postsTotalCount: 0, commentsTotalCount: 0 };
// 읽지 않은  알림 갯수
var unReadCounts = { postsUnReadCount: 0, commentsUnReadCount: 0 };

// ===================== JWT 파싱 =====================
function parseJwt(token) {
	// '토큰' 유효성 체크
    if (!token || typeof token !== 'string') {
		return null;
    }
    // JWT는 '헤더(lenth=1(index=0)),
	//      .페이로드(length=2(inedex=1))
	//      .서명(length=3(index=2))' 구조여야 함
	// JWT 토큰을 '.(닷)'을 기준으로 배열화
    var parts = token.split('.');
	// parts의 길이가 '3'이 아니라면 문제가 있으므로, return
    if (parts.length !== 3) {
		return null;
	}
    try {
		// 페이로드 모든 "-"문자 -> '+'문자로 변환 
		//       모든 "_"문자 -> '/'문자로 변환
        var payload = parts[1].replace(/-/g, '+').replace(/_/g, '/');
		// Base64 URL Safe 문자열을 표준 Base64로 변환 후 디코딩
        return JSON.parse(window.atob(payload)); // JSON 파싱 후 반환
    } catch (e) {
        return null;
    }
}

// ===================== 토큰 갱신 스케줄링 =====================
function scheduleTokenRefresh() {
    var token = localStorage.getItem('accessToken'); // 현재 액세스 토큰 가져오기
	// '토큰' 유효성 체크
    if (!token) {
		return;
	}

    var payload = parseJwt(token); // 토큰 페이로드 파싱
	// 페이로드 {"sub" : "user123","exp": 만료시간}유효성 검사 
	// null이거나 페이로드가 만료되었을경우
    if (!payload || !payload.exp) {
		return;
	}

    var exp = payload.exp * 1000; // 만료 시간을 밀리초로 변환
    var now = Date.now(); //현재 시간을 밀리초로 변환
    var refreshTime = exp - now - 10 * 60 * 1000; // 만료 10분 전 갱신 예정

    if (refreshTime <= 0) {
        // 이미 만료되었거나 갱신 시점이 지난 경우 바로 갱신
        refreshToken();
    } else {
        if (refreshTimer) clearTimeout(refreshTimer); // 이전 타이머 제거
        refreshTimer = setTimeout(refreshToken, refreshTime); // 갱신 예약
    }
}

// ===================== 토큰 갱신 =====================
function refreshToken(callback) {

	// 만약 리프레쉬 토큰이 존재하지 않다면은
	if (!localStorage.getItem('refreshToken')) {
	    console.error("refreshToken 없음 → 로그아웃 처리");
		// 로그아웃 처리 후
	    logout();
		// 프로세스 종료
	    return;
	}

    $.ajax({
        url: '/auth/refresh', 
        method: 'POST',
        headers: { 'Authorization': 'Bearer ' + localStorage.getItem('refreshToken') }, // 리프레시 토큰 사용
        success: function(res) {
            // 성공시 새 토큰 저장
            localStorage.setItem('accessToken', res.accessToken);
            localStorage.setItem('refreshToken', res.refreshToken);
            localStorage.setItem('role', res.role);
			localStorage.setItem('memberId', res.memberId);

            refreshRetryCount = 0; // 재시도 카운트 초기화
            scheduleTokenRefresh(); // 다음 갱신 스케줄링
			// 첫 refreshToken() 호출 실패(error)후 
			// 다시(callback) refreshToken()을 호출 했다면
            if (callback){
				// refreshToken()을 호출한 외부 함수로 'res.accessToken'를 반환 
				callback(res.accessToken)
			};
        },
        error: function(err) {
			// API 호출 실패후
            console.error('토큰 갱신 실패', err);

			// 'refreshToken()'이 'MAX_RETRY(3)'보다 작다면은,
            if (refreshRetryCount < MAX_RETRY) {
				// 증감후
                refreshRetryCount++;
				// 0.5초 후에 리프레쉬 토큰 호출,
				// refreshToken()을 콜백으로 사용하여 성공시, 
				// refreshToken()을 호출한 외부 함수로 response 반환
                window.setTimeout(() => refreshToken(callback), RETRY_INTERVAL); // 재시도
            } else {
                logout(); // 최대 재시도 실패 시 로그아웃
            }
        }
    });
}

// ===================== AJAX Wrapper =====================
function ajaxWithToken(options) {
	// 로컬스토리지에서 액세스 토큰 가져오기
    var token = localStorage.getItem('accessToken');

	// ajax 옵션 에서 'headers' 꺼내기, 없으면 빈 객체({})
    var headers = options.headers || {};

	// 토근이 있고, 헤더에'Authorization(key)'의 '값(Value)'이 없으면
    if (token && !headers['Authorization']) {
		//헤더에 토큰 셋팅
        headers.Authorization = 'Bearer ' + token; // 요청 헤더에 액세스 토큰 추가
    }

	//'...' spread연산자에 의해 AJAX객체(options)에 headers를 더하여 
	// ajax 새로운 옵션(options)객체 리턴
    return $.ajax({ ...options, headers: headers })
	//실패시
        .fail(function(xhr) {
			// 401(권한 없음(대부분 필터 문제))
            if (xhr.status === 401) {
                // 401 응답 발생 시 재시도 제한 확인

				// ajaxRetryCountMap 객체에는 [options.url(key)]의 값이 없음
				// 그러므로, 무조건 처음엔 undefinded이므로 '0'으로 초기화  
				ajaxRetryCountMap[options.url] = ajaxRetryCountMap[options.url] || 0;
				// 3번 ajax 다시 시도했다면,
                if (ajaxRetryCountMap[options.url] >= MAX_RETRY) {
					// 프로세스 종료
					return;
				}
				// 증감연산자에의해 0 -> 3까지 증가
                ajaxRetryCountMap[options.url]++;
                // refreshToken() 콜(호출) 백(응답(newToken)
				// 토큰 갱신 후 원래 ajax 요청 재시도
                refreshToken(function(newToken) {
                    options.headers = options.headers || {};
					// 헤더의 "Authorization"값이 매번 바뀌므로 '[]브래킷' 표기법 사용 
                    options.headers['Authorization'] = 'Bearer ' + newToken;
					// ajax 다시시도
                    $.ajax(options);
                });
            }
        });
}

// 헤더 사용자 메뉴 업데이트
function updateUserMenu() {
    var token = localStorage.getItem('accessToken');

    // 토큰 없으면 로그인/회원가입 버튼 보여주기
    if (!token) {
        $('#user-actions').html(`
            <button id="login-btn">Log_in</button>
            <button id="register-btn">Log_up</button>
        `);
        $('#login-btn').off('click')
		               .on('click', function() {
						 window.location.href = '/signin';
					 });
        $('#register-btn').off('click')
		                  .on('click', function() {
							 window.location.href = '/signup_consent'; 
						 });
        return;
    }

    // 토큰 있으면 회원 정보 가져오기
    ajaxWithToken({
        url: '/members/me',
        type: 'GET',
        success: function(res) {
			memberInfo = res;
            renderUserMenu(memberInfo);
        },
        error: function(xhr) {
            alert.error(xhr.responseText);
            // 인증 실패 시 토큰 제거 후 다시 로그인/회원가입 버튼 표시
            localStorage.removeItem('accessToken');
            updateUserMenu();
        }
    });
}

// 사용자 메뉴 렌더링
function renderUserMenu(memberInfo) {

	fetchUnreadNotificationsCount(function() {
	    var notificationCount = unReadCounts.postsUnReadCount + unReadCounts.commentsUnReadCount;


		// 게시글 알림만 켜져있으면
		if (memberInfo.postNotificationEnabled && !memberInfo.commentNotificationEnabled) {
		    notificationCount = unReadCounts.postsUnReadCount;
		}
		// 댓글 알림만 켜져있으면
		else if (!memberInfo.postNotificationEnabled && memberInfo.commentNotificationEnabled) {
		    notificationCount = unReadCounts.commentsUnReadCount;
		}
		// 둘 다 켜져있으면
		else if (memberInfo.postNotificationEnabled && memberInfo.commentNotificationEnabled) {
		    notificationCount = unReadCounts.postsUnReadCount + unReadCounts.commentsUnReadCount;
		}
		// 둘 다 꺼져있으면
		else {
		    notificationCount = 0;
		}

		if (notificationCount > 0) {
		    $("#delete-all-notifications").prop("disabled", false).removeClass("disabled-btn");
		    $("#mark-all-read").prop("disabled", false).removeClass("disabled-btn");
		} else {
		    $("#delete-all-notifications").prop("disabled", true).addClass("disabled-btn");
		    $("#mark-all-read").prop("disabled", true).addClass("disabled-btn");
		}

	    var userHtml = `
	        <span>안녕하세요, <strong>${memberInfo.nickname}</strong>님 (${memberInfo.memberGradeLevel})</span>
	        <button id="notification-btn">Log_🔔(${notificationCount})</button>
	        <button id="member-info-btn">Log_Me</button>
	        <button id="logout-btn">Log_Out</button>
	    `;
	    $('#user-actions').html(userHtml);

	    $("#member-info-btn").off('click').on('click',function(){
	        window.location.href ='/member_me';
	    });

	    $('#logout-btn').off('click').on('click', function() {
	        localStorage.removeItem('accessToken');
	        localStorage.removeItem('refreshToken');
	        localStorage.removeItem('memberId');
	        updateUserMenu();
	        window.location.href ="/";
	    });
	});
}

// ===================== 댓글 알림 갯수 =====================
function fetchUnreadNotificationsCount(callback) {
    var postCountUrl = "/notifications/count/posts/unread";
    var commentCountUrl = "/notifications/count/comments/unread";

    ajaxWithToken({
        url: postCountUrl,
        type: "GET",
        success: function(postCount) {
            unReadCounts.postsUnReadCount = postCount;

            ajaxWithToken({
                url: commentCountUrl,
                type: "GET",
                success: function(commentCount) {
                    unReadCounts.commentsUnReadCount = commentCount;

                    if (callback) callback(); // 데이터 갱신 후 렌더링 실행
                },
                error: function() {
                    console.error("댓글 알림 개수 조회 실패");
                }
            });
        },
        error: function() {
            console.error("게시글 알림 개수 조회 실패");
        }
    });
}

// 알림 버튼 클릭 이벤트
$(document).on('click', '#notification-btn', function(e) {
    e.stopPropagation();
    $('#notification-popup').toggle();

    if ($('#notification-popup').is(':visible')) {
        loadNotifications(); // 팝업이 열리면 알림 데이터 로드
    }
});

// 팝업 외부 클릭 시 닫기
$(document).on('click', function(e) {
    if (!$(e.target).closest('#notification-popup, #notification-btn').length) {
        $('#notification-popup').hide();
    }
});

function loadNotifications() {
    $("#posts-unread-count").empty();
    $("#comments-unread-count").empty();

    // 게시글 알림 사용 여부 체크
    if (!memberInfo.postNotificationEnabled) {
        $("#post-notification-list").html(`
            <li>게시글 알림이 꺼져 있습니다.</li>
            <button id="go-post-settings">알림 설정하러 가기</button>
        `);
        $("#posts-unread-count").text(0);
        $("#posts-total-count").text(0);
    } else {
        loadPostNotifications();
    }

    // 댓글 알림 사용 여부 체크
    if (!memberInfo.commentNotificationEnabled) {
        $("#comment-notification-list").html(`
            <li>댓글 알림이 꺼져 있습니다.</li>
            <button id="go-comment-settings">알림 설정하러 가기</button>
        `);
        $("#comments-unread-count").text(0);
        $("#comments-total-count").text(0);
    } else {
        loadCommentNotifications();
    }
}

// 게시글 알림
function loadPostNotifications() {

	// 안 읽은 게시글 알림
	$("#posts-unread-count").text(unReadCounts.postsUnReadCount);
	
    ajaxWithToken({
        url: "/notifications/count/posts",
        type: "GET",
        success: function(postTotal) {
			totalCounts.postsTotalCount = postTotal;

			// 게시글 알림이 100개 이상이면 "100+"로 표시
			if (totalCounts.postsTotalCount > 100) {
			    $("#posts-total-count").text("100+");
			} else {
			    $("#posts-total-count").text(totalCounts.postsTotalCount);
			}
        }
    });

	ajaxWithToken({
	    url: "/notifications/posts/recent",
	    type: "GET",
	    success: function(posts) {

	        let html = "";

	        if (!posts || posts.length === 0) {
				console.log("들어옴??");
	            html = `<li class="post-notification-no-data">최근 게시글 알림이 없습니다.</li>`;
	        } else {
	            posts.forEach(post => {
	                let href = (post.boardId === 1) ? `/board/${post.boardId}/notice/${post.postId}`
	                    							: `/board/${post.boardId}/normal/${post.postId}`;

	                let title = `${post.senderNickname}${post.notificationMessage}`;

	                html += `<li><a href="${href}" class="notification-link" data-notification-id="${post.notificationId}">${title}</a></li>`;
	            });
	        }

	        $("#post-notification-list").html(html);
	    }
	});
}

// 댓글 알림
function loadCommentNotifications() {

	// 안 읽은 댓글 알림
	$("#comments-unread-count").text(unReadCounts.commentsUnReadCount);

    ajaxWithToken({
        url: "/notifications/count/comments",
        type: "GET",
        success: function(commentTotal) {
			totalCounts.commentsTotalCount = commentTotal;

			// 게시글 알림이 100개 이상이면 "100+"로 표시
			if (totalCounts.commentsTotalCount > 100) {
			    $("#comments-total-count").text("100+");
			} else {
			    $("#comments-total-count").text(totalCounts.commentsTotalCount);
			}
        }
    });

    ajaxWithToken({
        url: "/notifications/comments/recent",
        type: "GET",
        success: function(comments) {

			let html = "";

			if (!comments || comments.length === 0) {
			    html = `<li class="comment-notification-no-data">최근 댓글 알림이 없습니다.</li>`;
			} else {
			    comments.forEach(comment => {
					let href = (comment.boardId === 1) ? `/board/${comment.boardId}/notice/${comment.postId}` 
					    							   : `/board/${comment.boardId}/normal/${comment.postId}`;
				    if (comment.commentId) {
				        href += `#comment-${comment.commentId}`;
				    }

					let title = `${comment.senderNickname}${comment.notificationMessage}`;

					html += `<li><a href="${href}" class="notification-link" data-notification-id="${comment.notificationId}">${title}</a></li>`;
				});
			}

			$("#comment-notification-list").html(html);
        }
    });
}

// 알림 단건 읽음 처리 (댓글 + 게시글 통합)
$(document).on("click", ".notification-link", function(e) {
    e.preventDefault(); // 기본 클릭 동작 방지
    const notificationId = $(this).data("notification-id");
    const href = $(this).attr("href");

    ajaxWithToken({
        url: `/notifications/read/${notificationId}`,
        type: "PATCH",
        success: function() {
            window.location.href = href; // 클릭한 알림 페이지로 이동
        },
        error: function(xhr) {
            alert(xhr.responseText);
        }
    });
});

// 알림 설정 페이지 이동
$(document).on("click", "#go-post-settings, #go-comment-settings", function() {
	sessionStorage.setItem("editMemberPrefill",  JSON.stringify(memberInfo));
	window.location.href = '/member_me_update';
});

// 알림 전체 보기 페이지 이동
$(document).on("click", ".notification-more-btn", function() {

	if (this.id === "notification-post-more-btn") {
	    window.location.href = "/notification?tab=post-notification";
	} else if (this.id === "notification-comment-more-btn") {
	    window.location.href = "/notification?tab=comment-notification";
	}
});


// 전체 알림 읽음 처리
$(document).on("click", "#mark-all-read", function() {
    if (!confirm("모든 알림을 읽음 처리하시겠습니까?")) return;

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
$(document).on("click", "#delete-all-notifications", function() {
    if (!confirm("모든 알림을 삭제하시겠습니까?")) return;

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

// ===================== 로그아웃 =====================
function logout() {
    localStorage.removeItem('accessToken'); // 액세스 토큰 삭제
    localStorage.removeItem('refreshToken'); // 리프레시 토큰 삭제
	localStorage.removeItem('memberId'); // 토큰 삭제
	updateUserMenu();
	window.location.href ="/";
}

// ===================== 페이지 로드 시 자동 시작 =====================
$(document).ready(function() {
	scheduleTokenRefresh(); // 페이지 로드 시 토큰 갱신 스케줄링 시작
	updateUserMenu();
});

