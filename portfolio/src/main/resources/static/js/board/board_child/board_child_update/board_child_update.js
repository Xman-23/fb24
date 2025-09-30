var token = localStorage.getItem('accessToken'); // 현재 액세스 토큰 가져오기
var userRole = localStorage.getItem('role');

//*************************************************** Function Start ***************************************************//
function check_child_board() {
	if(!parentBoardIds.includes(boardId) && !noticeBoard.includes(boardId)) {
		if(!token || userRole !== 'ROLE_ADMIN') {
		    window.location.href = "/";
		    return; // 여기서 함수 종료
		}
		getBoardInfo();
		board_update();
	}else {
        alert("잘못된 접근입니다. 일반 게시판이 아닙니다.");
        window.location.href = "/";
    }
}
//*************************************************** Function End ***************************************************//

// 게시판 정보 조회
function getBoardInfo() {
    if (!boardId || isNaN(boardId)) {
    	alert("올바른 boardId가 필요합니다."); 
    	return; 
    }
    $.getJSON(`/boards/${boardId}`, function(board) {
        $("#boardName").val(board.name);           // input value
        $("#boardDesc").val(board.description);    // textarea value
        $("#isActive").prop("checked", board.active); // checkbox checked
        $("#sortOrder").val(board.sortOrder);      // input value
        $("#parentNumber").val(board.boardId);
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
            parentBoardId: $("#parentNumber").val() ? Number($("#parentNumber").val()) : null
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

//*************************************************** 페이지 초기화 START ***************************************************//
$(document).ready(function() {
	check_child_board();
});
//*************************************************** 페이지 초기화 End ***************************************************//