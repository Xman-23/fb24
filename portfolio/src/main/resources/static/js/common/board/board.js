function getBoardFullHierarchy() {
    $.ajax({
        url : '/boards/hierarchy',
        type : 'GET',
        dataType : 'json',
        success : function(data) {

			// 공지게시판, 부모게시판을 나누기 위한 배열 객체 생성
			// -> boardsToRender[board(type:'notice')(0 Index), board(type:'parent')(1 Index)]
            var boardsToRender = [];

			//'...' spread연산자에 의해
            // 공지게시판 type('notice') 추가
            data.forEach(function(board){
				// 공지게시판에 'notice' type 부여
                if(board.boardId === 1){
                    boardsToRender.push({...board, type: 'notice'});
                }
            });

			//'...' spread연산자에 의해
            // 부모게시판 type('parent')추가 (9,14,15)
            data.forEach(function(board){
				// 부모 게시판 에 'parent'속성 부여
                if(board.boardId === 9 || board.boardId === 14 || board.boardId === 15
				   || board.boardId === 20){
                    boardsToRender.push({...board, type: 'parent'});
                }
            });
			
			// 게시판 렌더링
            renderBoards(boardsToRender);
        },
        error: function(xhr) {
            alert(xhr.responseText);
        }
    });
}

// 공지 + 부모 게시판 렌더링
function renderBoards(boards) {
    var container = $('#parent-boards');
    container.empty();

    boards.forEach(function(board) {
        var li = $('<li>').addClass('board-item'); // 공지와 부모 공통 클래스
		// 공지(notice)/ 부모게시판(popular) 링크
        var href = (board.type === 'notice') ? '/board_notice/' : '/board_popular/';
		// <a>태그에 'attr'로 링크 속성 부여 후 게시판 이름 'text'처리
        var a = $('<a>').attr('href', href + board.boardId)
                        .text(board.name);
		// 부모게시판 <li><a></a></li>
        li.append(a);

        // 자식 게시판 렌더링 (부모게시판만)
		// 자식게시판 유효성 체크
        if(board.type === 'parent' && board.childBoards && board.childBoards.length > 0) {
			// 자식 ul태그 생성(<ul></ul>)
            var ulChild = $('<ul>').addClass('child-boards');
            board.childBoards.forEach(function(child) {
				// 자식 li태그 생성(<li></li>)
                var liChild = $('<li>');
				// 자식 a태그 생성(<a></a>)
                var aChild = $('<a>').attr('href', '/board_normal/' + child.boardId)
                                     .text(child.name);
				//-> <li><a href="/board_normal/자식게시판ID">자식게시판 이름</a></li>
                liChild.append(aChild);
				//-> <ul class="child-boards"><li><a></a></li><ul>
                ulChild.append(liChild);
            });
			// 부모게시판(<li><a></a></li>).append(<ul class="child-boards"><li><a></a></li></ul>)
            li.append(ulChild);
        }
		//<ul id="parent-boards">
		//	<li><a></a>
		//		<ul class="child-boards">
		//			<li><a></a></li>
		//		</ul>
		//	</li>
		//</ul>
        container.append(li);
    });

    tabHover();
}

// 부모-자식 hover 시 자식 메뉴 열림/닫힘
function tabHover() {
    $(".parent-board").hover(
        function() {
            $(this).children(".child-boards").stop(true, true).slideDown(200);
        },
        function() {
            $(this).children(".child-boards").stop(true, true).slideUp(200);
        }
    );
}

// 실행
$(document).ready(function() {
    getBoardFullHierarchy();
});
