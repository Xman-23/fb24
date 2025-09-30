//*************************************************** 변수 START ***************************************************//
// 세션에서 토큰 가져오기
var token = sessionStorage.getItem("findEmailToken");

//*************************************************** 변수 End ***************************************************//

//*************************************************** Function Start ***************************************************//

function login_move() {
	// 로그인 이동 버튼
    $("#go-login").on("click", function() {
        window.location.href = "/signin";
    });
}

//*************************************************** Function Start ***************************************************//

//*************************************************** API START ***************************************************//

function find_id_two_api() {
    if (!token) {
        alert("먼저 1단계를 완료해주세요.");
        window.location.href = "/find_id_one";
    } else {
        // 서버에서 아이디 조회
        $.ajax({
            url: "/members/show-email",
            type: "GET",
            headers: {'Authorization': 'Bearer ' + token},
            dataType: 'json',
            success: function(res) {
                $("#user-id").html("회원님의 아이디는 <strong>" + res.email + "</strong> 입니다.");
            },
            error: function(xhr) {
                alert("아이디 조회 실패: " + xhr.responseText);
                //window.location.href = "/find_id_one";
            }
        });
    }
}

//*************************************************** API START ***************************************************//

$(document).ready(function() {
	login_move();

	find_id_two_api();
});