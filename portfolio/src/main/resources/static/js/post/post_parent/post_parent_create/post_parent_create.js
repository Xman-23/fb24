var token = localStorage.getItem('accessToken'); // 현재 액세스 토큰 가져오기

let allPreviews = [];  // 전체 preview DOM 저장
let files = [];
const maxCount = 10;   // 최대 업로드 개수 10개로 제한

//*****************************************Board ID Start*************************************************************
const pathParts = window.location.pathname.split("/");
const boardId = Number(pathParts[pathParts.length - 3]);
const noticeBoard = [1];
const parentBoardIds = [9, 14, 15, 20];
//*****************************************Board ID End*************************************************************

function check_parent_board_number(parentBoardIds) {
    if (parentBoardIds.includes(boardId) && !noticeBoard.includes(boardId)) {
		if(token) {
			child_load(boardId);
		}else {
			window.location.href ="/";
			return;
		}
    } else {
        alert("잘못된 접근입니다. 인기 게시판 글작성이 아닙니다.");
		window.location.href ="/";
		return;
    }
}

function child_load(parentId) {
    $.ajax({
        url: "/boards/" + parentId + "/hierarchy",
        type: "GET",
        headers: { "Authorization": "Bearer " + token },
        success: function (data) {
            $("#board_select").empty();
            $.each(data.childBoards, function (i, child) {
                var selected = (child.boardId === parentId) ? "selected" : "";
                $("#board_select").append(
                    `<option value="${child.boardId}" ${selected}>${child.name}</option>`
                );
            });
        }
    });
}

function post_create() {
    $("#post_create_form").off("submit").on("submit", function (e) {
        e.preventDefault();
        var formData = new FormData(this);

        ajaxWithToken({
            url: "/posts",
            type: "POST",
            data: formData,
            processData: false,
            contentType: false,
            success: function (response) {
				var responseBoardId = response.boardId;
				var reponsePostId = response.postId;
                alert("게시글이 등록되었습니다!");
                window.location.href = `/board/${responseBoardId}/normal/${reponsePostId}`;
            },
            error: function (xhr) {
                alert("에러 발생: " + xhr.responseText);
            }
        });
    });
}

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
    check_parent_board_number(parentBoardIds);
    image_visible();
    post_create();
});
