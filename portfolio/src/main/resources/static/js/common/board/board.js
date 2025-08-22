// 게시판 API
function getBoardFullHierarchy() {
	$.ajax({
		url : '/boards/hierarchy',
		type : 'GET',
		dataType : 'json',
		success : function(data) {

			var parentContainer = $('#board-tabs > ul');
			parentContainer.empty();

			data.forEach(function(parent) {

				// 부모게시판 
				var liParent = $('<li>').addClass('parent-board');
				var aParent = $('<a>').attr('href', '/boards/board/' + parent.id).text(parent.name);
				liParent.append(aParent);

				// 자식게시판 
				if(parent.childBoards && parent.childBoards.length > 0) {
					var ulChild = $('<ul>').addClass('child-boards');
					parent.childBoards.forEach(function(child) {
						//자식게시판 링크 걸기 
						var liChild = $('<li>'); // <li></li> 태그생성
						var aChild = $('<a>').attr('href','/boards/board/'+ child.id).text(child.name);

						liChild.append(aChild);
						ulChild.append(liChild);
						
					})
					//<li> 부모 게시판 
					//	<ul>
					//		<li>자식게시판></li>
					// 	<ul>
					//</li>
					liParent.append(ulChild);
				}
				
				//<ul>
				//		<li> 부모 게시판 
				// 			<ul>
				//				<li>자식게시판></li>
				// 			<ul>
				// 		</li>
				//</ul>
				// 부모게시판 + 자식게시판 합칠때마다 'parent-boards' append
				
				parentContainer.append(liParent);
			})

		},
		error: function(xhr) {
			alert(xhr.responseText); // 서버 예외 메세지 표시
		}
	})
}


function tabHover() {
    // =========================================
    // 부모 게시판(.parent-board)에 마우스를 올리면
    // 해당 부모의 자식 게시판(.child-boards)을 슬라이드로 보여주고
    // 마우스를 떼면 다시 숨기는 기능
    // =========================================
    
    $(".parent-board").hover(
        // 마우스가 부모 게시판 위로 올라갔을 때
        function() {
            // 현재 요소(this)의 직속 자식 중 .child-boards 선택
            // 진행 중인 애니메이션 중단하고 큐 초기화
            // 200ms 동안 아래로 슬라이드하며 보여주기
            $(this).children(".child-boards").stop(true, true).slideDown(200);
        },
        // 마우스가 부모 게시판에서 벗어났을 때
        function() {
            // 현재 요소(this)의 직속 자식 중 .child-boards 선택
            // 진행 중인 애니메이션 중단하고 큐 초기화
            // 200ms 동안 위로 슬라이드하며 숨기기
            $(this).children(".child-boards").stop(true, true).slideUp(200);
        }
    );
}

$(document).ready(function() {
	getBoardFullHierarchy();
	tabHover();
});