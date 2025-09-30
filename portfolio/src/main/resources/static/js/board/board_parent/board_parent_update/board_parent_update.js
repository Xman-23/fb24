//*************************************************** Function Start ***************************************************//
function check_parent_board_number() {
    if (parentBoardIds.includes(boardId) || noticeBoard.includes(boardId)) {
    	getBoardInfo(boardId);
    	board_update();
    } else {
        alert("잘못된 접근입니다. 인기 게시판이 아닙니다.");
        window.location.href = "/";
    }
}
//*************************************************** Function End ***************************************************//

	// 게시판 정보 조회
function getBoardInfo(boardId) {
    if (!boardId || isNaN(boardId)) {
    	alert("올바른 boardId가 필요합니다."); 
    	return; 
    }
    $.getJSON(`/boards/${boardId}`, function(board) {
        $("#boardName").val(board.name);           // input value
        $("#boardDesc").val(board.description);    // textarea value
        $("#isActive").prop("checked", board.active); // checkbox checked
        $("#sortOrder").val(board.sortOrder);      // input value
    }).fail(function(xhr) {
        console.error("게시판 단건 조회 실패:", xhr.responseText);
    });
}

function board_update() {
	// 수정 폼 제출
    $("#childBoardUpdateForm").on("submit", function(e) {
        e.preventDefault();
        const payload = {
            name: $("#boardName").val() || null,
            description: $("#boardDesc").val() || null,
            active: $("#isActive").is(":checked"),
            sortOrder: $("#sortOrder").val() ? Number($("#sortOrder").val()) : null,
            parentBoardId: null // 부모 게시판 수정 시 무조건 null
        };

        ajaxWithToken({
            url: `/boards/admin/${boardId}`,
            method: "PATCH",
            contentType: "application/json",
            data: JSON.stringify(payload),
            success: function(res) {
                $("#update-result").html(`<span style="color:green;">수정 성공: ${res.name}</span>`);
            },
            error: function(xhr) {
                $("#update-result").html(`<span style="color:red;">수정 실패: ${xhr.responseText}</span>`);
            }
        });
    });
}


//*************************************************** 페이지 초기화 Start ***************************************************//
$(document).ready(function() {
	check_parent_board_number();
});
//*************************************************** 페이지 초기화 End ***************************************************//