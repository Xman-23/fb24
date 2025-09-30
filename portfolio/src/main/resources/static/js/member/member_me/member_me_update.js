//*************************************************** 변수 START ***************************************************//
// 세션에서 회원정보 가져오기
var editMemberPrefill = JSON.parse(sessionStorage.getItem("editMemberPrefill"));
(editMemberPrefill);
//*************************************************** 변수 End ***************************************************//

//*************************************************** Function Start ***************************************************//
function token_check() {
	if (editMemberPrefill) {
	    $('#username').text(editMemberPrefill.username);
	    $('#nickname').val(editMemberPrefill.nickname);
	    $('#phoneNumber1').val(editMemberPrefill.phoneNumber.slice(0,3));
	    $('#phoneNumber2').val(editMemberPrefill.phoneNumber.slice(3,7));
	    $('#phoneNumber3').val(editMemberPrefill.phoneNumber.slice(7));
		// 주소 분리
		if (editMemberPrefill.address) {
		    // [10505] ... 구조에서 우편번호 추출
		    var match = editMemberPrefill.address.match(/^\[(\d+)\]\s*(.*)$/);
		    if (match) {
		        var postcode = match[1];  // 10505
		        var rest = match[2];      // "경기 고양시 ... 상세주소"

		        // rest를 공백 단위로 잘라서 마지막 토큰을 상세주소로 처리
		        var parts = rest.split(" ");
		        var detailAddress = parts.pop();     // "ㅇㄴㅇㄴㅁㅇㄴㅇ"
		        var baseAddress = parts.slice(1, -1).join(" ");    // "경기 고양시 덕양구 호수로 26-6 (토당동)"

		        $("#postcode").val(postcode);
		        $("#address").val(baseAddress);
		        $("#detailAddress").val(detailAddress);
		    } else {
		        // 혹시 [우편번호] 없이 저장된 경우 그대로 기본주소에 세팅
		        $("#address").val(editMemberPrefill.address);
		    }
		}
	    $('#memberGradeLevel').text(editMemberPrefill.memberGradeLevel);
	    $('#postNotification').prop('checked', editMemberPrefill.postNotificationEnabled);
	    $('#commentNotification').prop('checked', editMemberPrefill.commentNotificationEnabled);
    }else {
    	alert("회원정보가 없습니다");
    	window.location.href = "/";
    }
}

// 이벤트 바인딩
function input_binding() {
	$("#nickname, #phoneNumber1, #phoneNumber2, #phoneNumber3, #address").on("input", toggleSaveButton);
}
function checkbox_binding() {
	$("#postNotification, #commentNotification").on("change", toggleSaveButton);
}

// 닉네임 유효성 체크 (변경하지 않았으면 통과)
function checkNickNameValid() {
	// 기존 닉네임
    var currentNickname = editMemberPrefill.nickname;
	// 새로운 닉네임
    var newNickname = $("#nickname").val().trim();
	// 기존 닉네임과 새로운 닉네임이 같다면 그대로 true'
    if (newNickname === currentNickname) {
    	return true;
    }
	// 변경되었다면 중복확인의 결과인,
	// #nickname-feedbac 에서 data-valid 속성가져와 비교하기
    return $("#nickname-feedback").attr("data-valid") === "true";
}

// 닉네임 변경 여부 확인
function nicknameChanged() {
	// 닉네임 변경 안할시 false, 변경시 true
    return $("#nickname").val().trim() !== editMemberPrefill.nickname;
}

// 핸드폰 번호 변경 여부 확인
function phoneChanged() {
	// 핸드폰 변경 안할시 false, 변경시 true
    return getPhoneNumber() !== editMemberPrefill.phoneNumber;
}

// 주소 변경 여부 확인
function addressChanged() {
	// 주소 변경 안할시 false, 변경시 true
    return $("#address").val().trim() !== editMemberPrefill.address;
}

// 저장 버튼 활성화
function toggleSaveButton() {
    // 닉네임 유효성 체크 (변경하지 않았으면 통과)
    var nicknameValid = checkNickNameValid();

    // 핸드폰 번호 체크
    var phoneValid = checkPhoneNumber();

    // 주소 체크
    var address = $("#address").val().trim();
    var addressValid = address.length > 0;

    // 체크박스 변경 여부 체크
    var postNotificationChanged = $("#postNotification").is(":checked") !== editMemberPrefill.postNotificationEnabled;
    var commentNotificationChanged = $("#commentNotification").is(":checked") !== editMemberPrefill.commentNotificationEnabled;

    // 아무 것도 변경되지 않았으면 disabled
    var somethingChanged = nicknameValid && phoneValid && addressValid &&
                           (nicknameChanged() || phoneChanged() || addressChanged() || postNotificationChanged || commentNotificationChanged);

    $("#saveBtn").prop("disabled", !somethingChanged);
}

//*************************************************** Function End ***************************************************//

//*************************************************** API Start ***************************************************//
// 닉네임 중복 확인 API
function nickname_api() {
	$("#check-nickname-btn").on("click", function() {
	    var nickname = $("#nickname").val().trim();
	    if (!nickname) {
	    	return alert("닉네임을 입력해주세요.");
	    }

	    $.ajax({
	        url: "/members/check-nickname",
	        type: "GET",
	        data: { nickname: nickname },
	        success: function(res) {
	        	// 닉네임 중복확인
	            $("#nickname-feedback").text(res).css("color", "green").attr("data-valid", "true");
	            toggleSaveButton();
	        },
	        error: function(xhr) {
	        	// 닉네임 중복확인 실패
	            $("#nickname-feedback").text(xhr.responseText).css("color", "red").attr("data-valid", "false");
	            toggleSaveButton();
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


// 회원정보 변경 API
function member_info_update_api() {
	// 저장 버튼 클릭
	$("#saveBtn").on("click", function() {
	    if (!checkNickNameValid()) {
	    	return alert("닉네임이 유효하지 않습니다.");
	    }
	    if (!checkPhoneNumber()) {
	    	return alert("핸드폰 번호가 올바르지 않습니다.");
	    }
	    if (!$("#address").val().trim()) {
	    	return alert("주소를 입력해주세요.");
	    }
		// 주소 합치기
		var postcode = $("#postcode").val().trim();
		var address = $("#address").val().trim();
		var detailAddress = $("#detailAddress").val().trim();
		var fullAddress = (postcode ? "["+postcode+"] " : "") + address + " " + detailAddress;

	    var dto = {
	        nickname: $("#nickname").val().trim(),
	        phoneNumber: getPhoneNumber(),
	        address: fullAddress,
	        postNotificationEnabled: $("#postNotification").is(":checked"),
	        commentNotificationEnabled: $("#commentNotification").is(":checked")
	    };

	    ajaxWithToken({
	        url: '/members/me',
	        type: 'PATCH',
	        contentType: 'application/json',
	        data: JSON.stringify(dto),
	        success: function(res) {
	            alert(res);
	            window.location.href = '/member_me';
	        },
	        error: function(xhr) {
	            alert(xhr.responseText || '회원정보 수정에 실패했습니다.');
	        }
	    });
	});
}

//*************************************************** API End ***************************************************//

$(document).ready(function() {
	// 토큰 유효성 검증
	token_check();
	// 핸드폰번호 숫자 유효성 검증
	checkPhoneNumberDigit();
	// input 태그 바인딩
	input_binding();
	// checkbox 태그 바인딩
	checkbox_binding();

	nickname_api();
	member_info_update_api();
});