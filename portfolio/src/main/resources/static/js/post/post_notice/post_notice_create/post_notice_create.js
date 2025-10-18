
var token = localStorage.getItem('accessToken'); // 현재 액세스 토큰 가져오기
var userRole = localStorage.getItem('role');
let allPreviews = [];  // 전체 preview DOM 저장
let files = [];
const maxCount = 7;   // 최대 업로드 개수 10개로 제한


//*****************************************Board ID Start*************************************************************
const pathParts = window.location.pathname.split("/");
const noticeBoardId = Number(pathParts[pathParts.length - 3]);
const noticeBoard = [1];
//*****************************************Board ID End*************************************************************

function check_notice_board_number() {
    if (noticeBoard.includes(noticeBoardId)) {
		if(token && userRole === 'ROLE_ADMIN') {
			get_board_api(noticeBoardId);
		}else {
			window.location.href ="/";
			return;
		}
    } else {
        alert("잘못된 접근입니다. 공지 게시판 글작성이 아닙니다.");
		window.location.href ="/";
		return;
    }
}

// 게시판 정보 + 자식 게시판 조회
function get_board_api(noticeBoardId) {
	// 게시판ID 유효성 체크
    if (!noticeBoardId || isNaN(noticeBoardId)) {
    	alert("올바른 boardId가 필요합니다."); 
    	return; 
    }
	// 현재 게시판 정보 조회 API
    $.getJSON(`/boards/${noticeBoardId}`, function(board) {
		$("#board_select").empty();
        // 자식 게시판 조회 API
		var selected = (board.boardId === noticeBoardId) ? "selected" : "";
		$("#board_select").append(
		    `<option value="${board.boardId}" ${selected}>${board.name}</option>`
		);
    }).fail(function(xhr) {
    	console.error("게시판 단건 조회 실패:", xhr.responseText);
    });
}

function post_create() {
    $("#post_create_form").off("submit").on("submit", function (e) {
        e.preventDefault();
		// 현재 폼의 input값들을 FormDate 생성자로 넘겨줘서,
		// FromData 객체 생성
        var formData = new FormData(this);

        ajaxWithToken({
            url: "/posts",
            type: "POST",
            data: formData,
            processData: false,
            contentType: false,
            success: function (response) {

				// 체크박스 체크되어 있으면 한 번만 핀 설정 호출
				if ($('#pin_checkbox').is(':checked')) {
				    ajaxWithToken({
				        url: `/posts/${response.postId}/pin`,
				        type: 'PATCH',
				        contentType: 'application/json',
				        data: JSON.stringify({ pinned: true }),
				        success: function() {
				            console.log("핀 설정 완료");
				        },
				        error: function(xhr) {
				            alert("핀 설정 중 에러 발생: " + xhr.responseText);
				        }
				    });
				}
				var responseBoardId = response.boardId;
				var reponsePostId = response.postId;
				alert("게시글이 등록되었습니다!");
                window.location.href = `/board/${responseBoardId}/notice/${reponsePostId}`;
            },
            error: function (xhr) {
                alert("에러 발생: " + xhr.responseText);
            }
        });
    });
}

function show_admin() {
	// 관리자면 체크박스 표시
	if (userRole === 'ROLE_ADMIN') {
	    $('#admin').show();
	}
} 
/*
function initPinSetting(postId) {


	console.log("initPinSetting Start");

    // 체크박스 클릭 시 API 호출
    $('#pin_checkbox').on('change', function() {
        const pinned = $(this).is(':checked');

        $.ajax({
            url: `/posts/${postId}/pin`,
            type: 'PATCH',
            contentType: 'application/json',
            data: JSON.stringify({ pinned: pinned }),
            success: function() {
                alert(`게시글 ${pinned ? '핀 설정' : '핀 해제'} 완료`);
            },
            error: function(xhr) {
                alert("에러 발생: " + xhr.responseText);
            }
        });
    });
}
*/
function image_visible() {
    $("#images").on("change", function (event) {
        const newFiles = Array.from(event.target.files);

        newFiles.forEach((file) => {
            if (files.length >= maxCount) {
                alert(`최대 ${maxCount}개까지 업로드 가능합니다.`);
                return;
            }

            const isDuplicate = files.some(f => f.name === file.name);
            if (isDuplicate) {
                alert(`이미 업로드된 파일 이름입니다: ${file.name}`);
                return;
            }

            files.push(file);

            const reader = new FileReader();
            reader.onload = function (e) {
                const wrapper = $("<div>").addClass("preview-wrapper").data("file", file);
                const img = $("<img>").attr("src", e.target.result).addClass("preview-image");
                const removeBtn = $("<button>").attr("type","button").addClass("remove-image-btn").html("&times;");

                wrapper.append(img).append(removeBtn);
                allPreviews.push(wrapper);
                $("#image_preview_container").append(wrapper);

                // 파일 이름 갱신
                $("#file_names_container").empty();
                files.forEach((f, index) => {
                    const fileNameItem = $("<div>").addClass("file-name-item").text(f.name).data("file", f);
                    $("#file_names_container").append(fileNameItem);
                });

                // 이미지가 있으면 모두 삭제 버튼 표시
                if(files.length > 0){
                    $("#remove_all_images_btn").css("display", "inline-block");
                }
            };
            reader.readAsDataURL(file);
        });

        const dt = new DataTransfer();
        files.forEach(f => dt.items.add(f));
        $("#images")[0].files = dt.files;
    });
}

// 모든 이미지 삭제
$(document).on("click", "#remove_all_images_btn", function() {
    files = [];
    allPreviews = [];

    $("#image_preview_container").empty();
    $("#file_names_container").empty();

    $("#images")[0].value = "";

    // 버튼 숨기기
    $(this).css("display", "none");
});

// 이미지 삭제
$(document).on("click", ".remove-image-btn", function() {
    const wrapper = $(this).closest(".preview-wrapper");
    const file = wrapper.data("file");

    files = files.filter(f => f !== file);
    allPreviews = allPreviews.filter(w => w.data("file") !== file);

    wrapper.remove();
    $("#file_names_container").find(".file-name-item").filter(function(){return $(this).data("file")===file}).remove();

    const dt = new DataTransfer();
    files.forEach(f => dt.items.add(f));
    $("#images")[0].files = dt.files;
});

$(document).ready(function() {
    check_notice_board_number();
    image_visible();
    post_create();
	show_admin();
});
