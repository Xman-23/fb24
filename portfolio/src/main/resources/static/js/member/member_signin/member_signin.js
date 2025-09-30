//*************************************************** Function START ***************************************************//
// 이메일 셀렉트 박스 클릭 
function form_email_chang() {
	$("#email2").val("").prop("disabled", true);
	// select 박스 변경 시 
	$("#email-select").on("change", function() {
		// 해당 태그(email-select)의 value가져오기
	    var domain = $(this).val();

		// 직접 입력 옵션일 때
		if(domain ==="direct") {
			$("#email2").val("").prop("disabled", false).focus();
		} else {
			// 다른 옵션 선택 시
			// 'email-domain2' input value에 domain 값 덮어쓰기 
		    $("#email2").val(domain).prop("disabled", true);
		}
	});
}

// 회원가입 버튼 클릭 시
function form_signup() {
	$("#signup").on("click", function() {
		window.location.href = "/signup_consent";
	});
}

// 아이디 찾기 버튼 클릭 시
function form_find_id() {
	$("#find_id").on("click", function() {
		window.location.href = "/find_id_one";
	});
}

// 비밀번호 찾기 버튼 클릭 시
function form_find_password() {
	$("#find_password").on("click", function() {
		window.location.href = "/reset_password_one";
	});
}
//*************************************************** Function End ***************************************************//

//*************************************************** API START ***************************************************//

// 로그인 API
function memberLoginApi() {
	// 로그인 성공 후 실행
	const redirectUrl = localStorage.getItem("redirectAfterLogin");

	$('#auth-form').on('submit', function(e) {
	    e.preventDefault();

	    // 이메일 합치기
	    var email = getEmail();
	    var password = passwordTrim();

	    if(!email || !password){
	        alert("이메일과 비밀번호를 모두 입력해주세요.");
	        return;
	    }

	    var loginData = { email: email, password: password };

	    $.ajax({
	        url: '/auth/login',
	        type: 'POST',
	        contentType: 'application/json',
	        data: JSON.stringify(loginData),
	        success: function(res) {
	            localStorage.setItem('accessToken', res.accessToken);
	            localStorage.setItem('refreshToken', res.refreshToken);
	            localStorage.setItem('role', res.role);
	            localStorage.setItem('memberId', res.memberId);
				if (redirectUrl && redirectUrl.startsWith(window.location.origin)) {
				    // 이전 페이지로 이동
				    localStorage.removeItem("redirectAfterLogin"); // 사용 후 삭제
					alert('로그인 성공!');
				    window.location.href = redirectUrl;
				} else {
				    // 저장된 URL이 없으면 기본 홈으로 이동
					alert('로그인 성공!');
				    window.location.href = "/";
				}
	        },
	        error: function(xhr) {
	            alert(xhr.responseText);
	        }
	    });
	});
}
//*************************************************** API End ***************************************************//

//*************************************************** 페이지 초기화  Start ***************************************************//

$(document).ready(function() {
	form_email_chang();
	form_signup();
	form_find_id();
	form_find_password();

	memberLoginApi();
});

//*************************************************** 페이지 초기화  End ***************************************************//