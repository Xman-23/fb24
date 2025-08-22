// 헤더 사용자 메뉴 업데이트
function updateUserMenu() {
    var token = localStorage.getItem('accessToken');

    // 토큰 없으면 로그인/회원가입 버튼 보여주기
    if (!token) {
        $('#user-actions').html(`
            <button id="login-btn">로그인</button>
            <button id="register-btn">회원가입</button>
        `);
        $('#login-btn').on('click', function() { window.location.href = '/signin'; });
        $('#register-btn').on('click', function() { window.location.href = '/signup_consent'; });
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

	$("#member-info-btn").on('click',function(){
		window.location.href ='/member_me';
	})

    $('#logout-btn').on('click', function() {
        ajaxWithToken({
            url: '/auth/logout',
            type: 'POST',
            success: function(res) {
                alert(res);
                localStorage.removeItem('accessToken'); // 토큰 삭제
                updateUserMenu(); // 메뉴 초기화
				window.location.href ="/";
            },
            error: function(xhr) {
                console.error(xhr.responseText);
            }
        });
    });
}
// ===================== 페이지 로드 시 자동 시작 =====================
$(document).ready(function() {
	updateUserMenu();
});