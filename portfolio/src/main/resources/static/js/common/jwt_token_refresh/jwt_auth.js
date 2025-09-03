// ===================== 전역 변수 =====================
var refreshTimer = null;        // setTimeout 중복 방지용 타이머 변수
var refreshRetryCount = 0;      // 자동 갱신 재시도 카운트
var MAX_RETRY = 3;              // 토큰 갱신 최대 재시도 횟수
var RETRY_INTERVAL = 5000;      // 실패 시 재시도 간격 (밀리초 단위)
var ajaxRetryCountMap = {};     // AJAX 요청별 401 재시도 카운트 저장
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
            <button id="login-btn">로그인</button>
            <button id="register-btn">회원가입</button>
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
        success: function(memberInfo) {
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
    var userHtml = `
        <span>안녕하세요, <strong>${memberInfo.nickname}</strong>님 (${memberInfo.memberGradeLevel})</span>
		<button id="member-info-btn">내 정보</button>
        <button id="logout-btn">로그아웃</button>
    `;

    $('#user-actions').html(userHtml);

	$("#member-info-btn").off('click').on('click',function(){
		window.location.href ='/member_me';
	})

    $('#logout-btn').off('click').on('click', function() {
		localStorage.removeItem('accessToken'); // 토큰 삭제
		localStorage.removeItem('refreshToken'); // 토큰 삭제
		localStorage.removeItem('memberId'); // 토큰 삭제
		updateUserMenu(); // 메뉴 초기화
		window.location.href ="/";
    });
}

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


