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
    // JWT는 '헤더(lenth=1).페이로드(length=2).서명(length=3)' 구조여야 함
    var parts = token.split('.');
    if (parts.length !== 3) {
		return null;
	}
    try {
		// 헤더에 모든 "-"문자 -> '+'문자로 변환 
		//      모든 "_"문자 -> '/'문자로 변환
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
	// 페이로드 유효성 검사
    if (!payload || !payload.exp) {
		return;
	}

    var exp = payload.exp * 1000; // 만료 시간을 밀리초로 변환
    var now = Date.now();
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
    $.ajax({
        url: '/auth/refresh', 
        method: 'POST',
        headers: { 'Authorization': 'Bearer ' + localStorage.getItem('refreshToken') }, // 리프레시 토큰 사용
        success: function(res) {
            // 새 토큰 저장
            localStorage.setItem('accessToken', res.accessToken);
            localStorage.setItem('refreshToken', res.refreshToken);

            refreshRetryCount = 0; // 재시도 카운트 초기화
            scheduleTokenRefresh(); // 다음 갱신 스케줄링

            if (callback) callback(res.accessToken); // 콜백 호출
        },
        error: function(err) {
            console.error('토큰 갱신 실패', err);

            if (refreshRetryCount < MAX_RETRY) {
                refreshRetryCount++;
                setTimeout(() => refreshToken(callback), RETRY_INTERVAL); // 재시도
            } else {
                logout(); // 최대 재시도 실패 시 로그아웃
            }
        }
    });
}

// ===================== AJAX Wrapper =====================
function ajaxWithToken(options) {
    var token = localStorage.getItem('accessToken');
    var headers = options.headers || {};

    if (token && !headers['Authorization']) {
        headers['Authorization'] = 'Bearer ' + token; // 요청 헤더에 액세스 토큰 추가
    }

	//'...' spread연산자에 의해 AJAX객체(options)에 headers를 더하여 
	// ajax 새로운 옵샨(options)객체 리턴
    return $.ajax({ ...options, headers: headers })
        .fail(function(xhr) {
            if (xhr.status === 401) {
                // 401 응답 발생 시 재시도 제한 확인
                ajaxRetryCountMap[options.url] = ajaxRetryCountMap[options.url] || 0;
                if (ajaxRetryCountMap[options.url] >= MAX_RETRY) return;

                ajaxRetryCountMap[options.url]++;
                // 토큰 갱신 후 원래 요청 재시도
                refreshToken(function(newToken) {
                    options.headers = options.headers || {};
                    options.headers['Authorization'] = 'Bearer ' + newToken;
                    $.ajax(options);
                });
            }
        });
}

// ===================== 페이지 로드 시 자동 시작 =====================
$(document).ready(function() {
    scheduleTokenRefresh(); // 페이지 로드 시 토큰 갱신 스케줄링 시작
});

// ===================== 로그아웃 =====================
function logout() {
    localStorage.removeItem('accessToken'); // 액세스 토큰 삭제
    localStorage.removeItem('refreshToken'); // 리프레시 토큰 삭제
    window.location.href = '/signin'; // 로그인 페이지로 이동
}
