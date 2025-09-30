// DOM 로드 후 이벤트 바인딩
$(document).ready(function() {
    $('#create-board-btn').off('click').on('click', function() {
        console.log("게시판 생성 버튼 클릭");

        const name = $('#board-name').val().trim();
        const description = $('#board-description').val().trim();

        if (!name) {
            alert("게시판 이름은 필수입니다.");
            return;
        }

        const requestData = {
            name: name,
            description: description,
            parentBoardId: null // 부모 게시판
        };

        ajaxWithToken({
            url: '/boards/admin/create-board',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(requestData),
            success: function(res) {
                $('#create-result').html(`<span style="color:green;">게시판이 생성되었습니다: ${res.name}</span>`);
                $('#board-name').val('');
                $('#board-description').val('');
            },
            error: function(xhr) {
                $('#create-result').html(`<span style="color:red;">${xhr.responseText || '게시판 생성 실패'}</span>`);
            }
        });
    });
});
