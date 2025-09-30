//*************************************************** 변수 START ***************************************************//
// 필수 동의
var requiredConsents = ["TERMS_OF_SERVICE", "PRIVACY_POLICY"];

//*************************************************** 변수 End ***************************************************//

// 전체 동의 체크/해제
function all_cheked() {
	$("#check-all").on("change", function() {
        var isChecked = $(this).is(":checked");
        $("input[name='consents']").prop("checked", isChecked);
        toggleNextButton();
    });
}

// 각 체크박스 클릭 시 버튼 상태 업데이트
function cheked_update() {
    $("input[name='consents']").on("change", function() {
        toggleNextButton();
        // 개별 체크박스가 모두 체크되면 전체동의도 체크됨
        var allChecked = $("input[name='consents']").length === $("input[name='consents']:checked").length;
        $("#check-all").prop("checked", allChecked);
    });
}

// 다음 버튼 활성/비활성 함수
function toggleNextButton() {
	// 체크 박스 필수 요소(TERMS_OF_SERVICE", "PRIVACY_POLICY) 체크시 다음 버튼 활성화를 위해 필요한 변수
    var allRequiredChecked = true;

	// requiredConsents = ["TERMS_OF_SERVICE", "PRIVACY_POLICY"];
    requiredConsents.forEach(function(value) {
    	// value = TERMS_OF_SERVICE", "PRIVACY_POLICY 태그가 체크가 안되어있으면 
        if (!$("input[value='" + value + "']").is(":checked")) {
        	// 버튼 비활성화
            allRequiredChecked = false;
        }
    });

    // 다음 버튼 비활성화
    $("#next-btn").prop("disabled", !allRequiredChecked);
}

function next_signup() {
	// 다음 버튼 클릭 시 체크된 동의 항목 sessionStorage에 저장 후 회원가입 페이지로 이동
	$("#next-btn").on("click", function() {
							   // 체크된 input 태그 요소 가져온 후
	    var selectedConsents = $("input[name='consents']:checked")
	    					   // map으로 태그 -> value로 리턴(Jquery 객체)
	                           .map(function() { return this.value; })
	                           // value(Jquery객체) -> value(자바스크립트)
	                           .get();

	    // Json.stringify = JS객체 -> JSON문자열
	    // 세션스토리지에 key(consents), value(Json(selectedConsents)) 으로 저장
	    sessionStorage.setItem("consents", JSON.stringify(selectedConsents));
	    // 회원가입 페이지 이동
	    window.location.href = "/signup";
	});
}

//*************************************************** 페이지 초기화  Start ***************************************************//

$(document).ready(function() {
	/* 페이지 로드 시 초기 상태 */
	toggleNextButton();

	/* 뒤로가기/앞으로가기 시 체크 상태 기반으로 버튼 활성화 */
	$(window).on('pageshow', function() {
	    // 체크박스 상태에 따라 전체동의 체크 여부 갱신
	    var allChecked = $("input[name='consents']").length === $("input[name='consents']:checked").length;
	    $("#check-all").prop("checked", allChecked);

	    // 다음 버튼 활성화/비활성화
	    toggleNextButton();
	});

	/* 전체 동의 체크박스 */
	all_cheked();
	/* 체크 박스 선택할 때 마다, 다음 버튼 활성화 여부 체크 */
	cheked_update();
	/* 필수 체크박스 또는 전체 동의시 다음 페이지로 이동 */
	next_signup();
});
