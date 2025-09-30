//*************************************************** 페이지 초기화  Start ***************************************************//

// 버튼 활성화
function checkNextButton() {
    $("#password, #confirmPassword")
    .on("input", function() {
        checkPasswordValidity(); // 패스워드 유효성 검사
        checkPasswordMatch();  // 패스워드, 패스워드 확인 유효성 검사
        toggleResetButton(); // 종합 유효성 검증
    });
}

// 버튼 활성/비활성 토글
function toggleResetButton() {
	var passwordValid = checkPasswordValidity() && checkPasswordMatch();
    $("#reset-password-btn").prop("disabled", !(passwordValid));
}

//*************************************************** API START ***************************************************//

// 패스워드 변경 api
function password_reset_api() {
    // 버튼 클릭 시 PATCH 요청
    $("#reset-password-btn").on("click", function() {
        var  token = sessionStorage.getItem("resetPasswordToken");
        if(!token) {
            alert("토큰이 만료되었거나 없습니다. 다시 시도해주세요.");
            return;
        }

        var data = {
           newPassword: $("#password").val(),
           confirmNewPassword: $("#confirmPassword").val()
        };

        $.ajax({
            url: "/members/reset-password",
            type: "PATCH",
            contentType: "application/json",
            headers: { "Authorization": "Bearer " + token },
            data: JSON.stringify(data),
            success: function(res) {
                alert(res);
                window.location.href = "/signin";
            },
            error: function(xhr) {
                alert(xhr.responseText);
            }
        });
    });
}
// ===================== 페이지 로드 시 자동 시작 =====================
$(document).ready(function() {
	// 페이지 진입시 버튼 초기화
	toggleResetButton();
	// 버튼 활성화
	checkNextButton();
	// api
	password_reset_api();
});