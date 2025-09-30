//*************************************************** API Start ***************************************************//
function find_id_one_api() {

    // 폼 제출 이벤트 ("Enter"key로 폼 제출가능)
    $("#nextBtn").on("click", function() {
		// e.preventDefault() (form 사용시 사용. ) 
        $("#error-message").text(""); // 초기화

        if(!checkUserName()) {
        	$("#username").focus();
            $("#error-message").text("사용자 이름을 올바르게 입력해주세요.");
            return;
        }

        if (!checkResidentNumber()) {
        	$("#residentNumber1").focus();
            $("#error-message").text("주민번호를 올바르게 입력해주세요.");
            return;
        }

        // DTO Request
        var data = {
            username: getUserNameValue(),
            residentNumber: getResidentNumber()
        };

        $.ajax({
            url: "/members/find-email",
            type: "POST",
            contentType: "application/json",
            data: JSON.stringify(data),
            success: function(res) {
            	(res);
            	// sessionStorage("findEmailToken(key)", "TENP_TOKEN(value)") 저장
                sessionStorage.setItem("findEmailToken", res.token); // 임시 토큰 저장
                window.location.href = "/find_id_two"; // 2단계 페이지로 이동
            },
            error: function(xhr) {
                $("#error-message").text(xhr.responseText || "서버 오류가 발생했습니다.");
            }
        });
    });
}
//*************************************************** API End ***************************************************//

//*************************************************** Function Start ***************************************************//

// input 이벤트 등록
function checkNextButton() {
    $("#username, #residentNumber1, #residentNumber2").on("input", function() {
        toggleNextButton();
    });
}

// 입력값 검증 후 버튼 활성/비활성화
function toggleNextButton() {
    var usernameFilled = checkUserName();
    var residentFilled = checkResidentNumber();
    $("#find-email-form button").prop("disabled", !(usernameFilled && residentFilled));
}

//*************************************************** Function End ***************************************************//

$(document).ready(function() {
	/* 첫 진입시 버튼 비활성화 */
	toggleNextButton();
	/* 뒤로가기/앞으로가기 시에도 버튼 상태 체크 */
	$(window).on('pageshow', function() {
	    toggleNextButton();
	});

	checkNextButton();
	// 주민번호 숫자 체크 */
	checkResidentNumberDigit();

	find_id_one_api();
});