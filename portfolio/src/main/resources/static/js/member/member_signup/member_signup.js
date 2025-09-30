//*************************************************** 변수 START ***************************************************//

// sessionStorge에서 key(consents)로 체크 박스 value 꺼내기 
// JSON.stringify(문자열) -> JSON.parse(자바스크립트객체(배열))
var consents = JSON.parse(sessionStorage.getItem("consents") || "[]");

//*************************************************** 변수 End ***************************************************//

//*************************************************** Function Start ***************************************************//

function checkNextButton() {
    $("#username,#email1,#email2,#nickname,#password, #confirmPassword, #residentNumber1, #residentNumber2, #phoneNumber1, #phoneNumber2, #phoneNumber3")
    .on("input", function() {
        checkPasswordValidity(); // 패스워드 유효성 검사
        checkPasswordMatch();  // 패스워드, 패스워드 확인 유효성 검사
        toggleSignupButton(); // 종합 유효성 검증
    });
}

   // 이메일 도메인(셀렉트 박스) 선택
   function form_email_chang() {
   	$("#email2").val("").prop("disabled", true);
    $("#email-domain-select").on("change", function() {
    	// 도메인 변경시 feedback 초기화
	    $("#email-feedback").text("");
	    $("#email-feedback").css("color", "");
        var domain = $(this).val();
		if(domain ==="direct") {
			$("#email2").val("").prop("disabled", false).focus();
		} else {
			// 다른 옵션 선택 시
			// 'email-domain2' input value에 domain 값 덮어쓰기 
		    $("#email2").val(domain).prop("disabled", true);
		}
        toggleSignupButton();
    });
}

function check_email() {
	$("#email1, #email2").on("input", function() {
	    $("#email-feedback").text("");
	    $("#email-feedback").css("color", "");
	    toggleSignupButton(); // 버튼 상태 갱신
	});

}

function check_nickname() {
	$("#nickname").on("input", function() {
	    $("#nickname-feedback").text("");
	    $("#nickname-feedback").css("color", "");
	    toggleSignupButton(); // 버튼 상태 갱신
	});

}

function checkNumber() {
	// 주민번호 숫자만 입력
	checkResidentNumberDigit();
	// 핸드폰번호 숫자만 입력
	checkPhoneNumberDigit();
}

   //회원가입 버튼 활성/비활성
   function toggleSignupButton() {
   	var nameValid = checkUserName();
       var emailValid = checkEmailValid();
       var nicknameValid = checkNickNameValid();
       var passwordValid = checkPasswordValidity() && checkPasswordMatch();
       var residentValid = checkResidentNumber();
       var phoneValid = checkPhoneNumber();
       // 'consents' 에 ["TERMS_OF_SERVICE", "PRIVACY_POLICY"] 포함(includes)되어있으면 'true'
       var consentsAgreed = consents.includes("TERMS_OF_SERVICE") && consents.includes("PRIVACY_POLICY");

       // 회원가입 버튼 활성화/비활성화(disabled) 여부
       $("#signup-btn").prop("disabled", !(nameValid&&emailValid && nicknameValid && passwordValid && residentValid && phoneValid && consentsAgreed));
   }

//*************************************************** Function End ***************************************************//

//*************************************************** API START ***************************************************//

   // 이메일 중복 확인 API
   function emailApi() {
       $("#check-email-btn").on("click", function() {
           var email1 = $("#email1").val().trim();
           var email2 = $("#email2").val().trim();
           if (!email1 || !email2) return alert("이메일을 모두 입력해주세요.");

           var email = email1 + "@" + email2;

           $.ajax({
               url: "/members/check-email",
               type: "GET",
               data: { email: email },
               success: function(res) {
                   $("#email-feedback").text(res).css("color", "green").attr("data-valid", "true");
                   toggleSignupButton();
               },
               error: function(xhr) {
                   $("#email-feedback").text(xhr.responseText).css("color", "red").attr("data-valid", "false");
                   toggleSignupButton();
               }
           });
       });
	}

   // 닉네임 중복 확인 API
   function nicknameApi() {
       $("#check-nickname-btn").on("click", function() {
           var nickname = $("#nickname").val().trim();
           if (!nickname) return alert("닉네임을 입력해주세요.");

           $.ajax({
               url: "/members/check-nickname",
               type: "GET",
               data: { nickname: nickname },
               success: function(res) {
                   $("#nickname-feedback").text(res).css("color", "green").attr("data-valid", "true");
                   toggleSignupButton();
               },
               error: function(xhr) {
                   $("#nickname-feedback").text(xhr.responseText).css("color", "red").attr("data-valid", "false");
                   toggleSignupButton();
               }
           });
       });
   }

// 주소 입력
$(document).on("click", "#search-address-btn", function() {
	new daum.Postcode({
	    oncomplete: function(data) {
	        var roadAddr = data.roadAddress; 
	        var extraAddr = '';

	        if(data.bname !== '' && /[동|로|가]$/g.test(data.bname)){
	            extraAddr += data.bname;
	        }
	        if(data.buildingName !== '' && data.apartment === 'Y'){
	            extraAddr += (extraAddr !== '' ? ', ' + data.buildingName : data.buildingName);
	        }
	        if(extraAddr !== ''){
	            extraAddr = ' (' + extraAddr + ')';
	        }

	        $("#postcode").val(data.zonecode);
	        $("#address").val(roadAddr + extraAddr);
	        $("#detailAddress").focus();
	    }
	}).open();
});

// 회원가입 API
function signupApi() {
	$("#signup-btn").on("click", function() {
        // 주민번호 합치기
        var residentNumber = getResidentNumber();

        // 핸드폰 번호 합치기
        var phoneNumber = getPhoneNumber();

        // 이메일 합치기
        var email = getEmail();
		
		// 주소 합치기
		var postcode = $("#postcode").val().trim();
		var address = $("#address").val().trim();
		var detailAddress = $("#detailAddress").val().trim();
		var fullAddress = (postcode ? "["+postcode+"] " : "") + address + " " + detailAddress;

        // JSON 객체 '{key : vlaue}'
        var memberData = {
            username: getUserNameValue(),
            email: email,
            nickname: $("#nickname").val().trim(),
            password: passwordTrim(),
            phoneNumber: phoneNumber,
            residentNumber: residentNumber,
            address: fullAddress,
            consents: consents
        };

        $.ajax({
            url: "/members/signup",
            type: "POST",
            contentType: "application/json",
            data: JSON.stringify(memberData),
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

//*************************************************** API End ***************************************************//

$(document).ready(function() {
	form_email_chang();
	checkNextButton();
	checkNumber();
	check_email();
	check_nickname();

	emailApi();
	nicknameApi();
	signupApi();
});