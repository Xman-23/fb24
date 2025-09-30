//*************************************************** Function Start ***************************************************//

// 사용자 이름 입력 이벤트
function check_user_name() {
	$("#username").on("input", toggleFindPasswordButton);
}

// doamin 변경시  toggleFindPasswordButton(); 다시 호출
   function form_email_chang() {
   	$("#email2").val("").prop("disabled", true);
       // 이메일 도메인 선택 시 input 자동 변경
       $("#email-domain-select").on("change", function() {
   		// 도메인 변경시 feedback 초기화
   	    $("#email-feedback").text("");
   	    $("#email-feedback").css("color", "");
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
           toggleFindPasswordButton();
       });
   }

function check_email() {
	$("#email1,#email2").on("input", function() {
		// 이메일1 변경시 피드백 초기화
	    $("#email-feedback").text("");
	    $("#email-feedback").css("color", "");
	    toggleFindPasswordButton(); // 버튼 상태 갱신
	});

}

   // 주민번호 숫자만 입력
   function check_residentnumber() {
       $('#residentNumber1, #residentNumber2').on('input', function() {
           this.value = this.value.replace(/\D/g, '');
           toggleFindPasswordButton();
       });
   }

   // 버튼 활성/비활성 토글
   function toggleFindPasswordButton() {
       var isValid = checkUserName() && checkEmailValid() && checkResidentNumber();
       $("#find-password-btn").prop("disabled", !isValid);
   }
 	//*************************************************** Function End ***************************************************//

 	//*************************************************** API START ***************************************************//

function checkEmailApi() {
    // 이메일 확인
    $("#check-email-btn").on("click", function() {
        var email1 = $("#email1").val().trim();
        var email2 = $("#email2").val().trim();

        if (!email1 || !email2) {
            alert("이메일을 모두 입력해주세요.");
            return;
        }

        var email = email1 + "@" + email2;

        $.ajax({
            url: "/members/check-email",
            type: "GET",
            data: { email: email },
            success: function() {
                // 존재하면 초록 → 유효
				$("#email-feedback").text("등록되지 않은 이메일입니다.").css("color", "rgb(255, 0, 0)").attr("data-valid", "false");
                toggleFindPasswordButton();
            },
			error: function(xhr) {
			    if (xhr.responseText === "이메일이 유효하지 않습니다.") {
			        $("#email-feedback")
			            .text("등록되지 않은 이메일입니다.")
			            .css("color", "rgb(255, 0, 0)")
			            .attr("data-valid", "false");
			    } else {
			        // 기존 처리
			        $("#email-feedback")
			            .text("가입된 이메일이 확인되었습니다.")
			            .css("color", "rgb(0, 128, 0)")
			            .attr("data-valid", "true");
			    }
			    toggleFindPasswordButton();
			}
        });
    });
}


   // 다음 버튼 클릭
   function reset_password_api() {
       $("#find-password-btn").on("click", function() {
           var username = $("#username").val().trim();
           
           if (!checkUserName()) {
           	$('#username').focus();
           	$("#error-message").text("사용자 이름을 올바르게 입력해주세요.");
           	return
           }
           if (!checkEmailValid()) {
           	$('#email1').focus();
           	$("#error-message").text("이메일을 올바르게 입력해주세요.");
           	return
           }
           if (!checkResidentNumber()) {
           	$("#residentNumber1").focus();
           	$("#error-message").text("주민번호를 올바르게 입력해주세요.");
           	return
           }

           var data = {
               email: getEmail(),
               username: username,
               residentNumber: getResidentNumber()
           };

           $.ajax({
               url: "/members/reset-password-token",
               type: "POST",
               contentType: "application/json",
               data: JSON.stringify(data),
               success: function(res) {
                   // 토큰을 sessionStorage에 저장
                   (res);
                   sessionStorage.setItem("resetPasswordToken", res.token);
                   // 2단계 페이지로 이동
                   window.location.href = "/reset_password_two";
               },
               error: function(xhr) {
                   alert(xhr.responseText);
               }
           });
       });
   }

 	//*************************************************** API START ***************************************************//

$(document).ready(function() {

	// 진입시 버튼 비활성화
	toggleFindPasswordButton();
	/* 뒤로가기/앞으로가기 시에도 버튼 상태 체크 */
	$(window).on('pageshow', function() {
		toggleFindPasswordButton();
	});
	check_user_name();
	form_email_chang();
	check_email();
	check_residentnumber();

	checkEmailApi();
	reset_password_api();
});