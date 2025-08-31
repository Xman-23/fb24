// 로그인 버튼
function signin() {
    $('#login-btn').off('click').on('click', function() {
        // "/signin"으로 요청 → WebMvcConfig에서 forward 처리
        window.location.href = '/signin';
    });
}

// 회원가입 버튼

function signup() {
	$('#register-btn').off('click').on('click',function(){

		// "/signup"으로 요청 → WebMvcConfig에서 forward 처리
		window.location.href = '/signup_consent';
	})
};


// ===================== 페이지 로드 시 자동 시작 =====================
$(document).ready(function() {
    signin();
	signup();
});
