// 타이머 변수 
var debounceTimer;
var token = localStorage.getItem('accessToken'); // 현재 액세스 토큰 가져오기

//*****************************************Board ID Start*************************************************************
// 도메인에서 '/'기준으로 배열화[] 5 -1  
const pathParts = window.location.pathname.split("/");
// 도메인 배열에서 2번째 인덱스 boardId 가져오기
const boardId = Number(pathParts[pathParts.lenth -3]);
// 도메인에서 마지막 인덱스 postId 가져오기
const postId = Number(pathParts[pathParts.length - 1]);
// 공지게시판 BoardId
const noticeBoard = [1];
// 부모게시판 BoardId
const parentBoardIds = [9, 14, 15, 20];
//*****************************************Board ID End*************************************************************
//***************************************** 게시글 Start ************************************************************* 

//***************************************** 게시글 End *************************************************************
//*****************************************No Comment Start************************************************************* 
const no_main_popularList = "메인 인기 게시글이 없습니다.";
const no_popularList = "인기 게시글이 없습니다.";
const no_normalList = "게시글이 없습니다.";
const no_searchList = "검색 결과가 없습니다.";
const no_fin_noticeList ="고정된 공지 게시글이 없습니다.";
const no_noticeList ="공지 게시글이 없습니다.";
//*****************************************No Comment End*************************************************************

//*****************************************Function Start*************************************************************
// 게시글이 없는 경우 만들어줄 '<li></li>'태그
function no_posts_tag(string) {

	var no_posts_html = `
							<li class= "no-posts">
								${string}
							</li>
						`
	return no_posts_html;

}

// 게시글 유효성 체크
function check_posts(posts) {
	return !posts || posts.length === 0;
}

// 메인, 부모 , 자식 게시글 공용으로 사용
function shwo_image_download(imageUrls) {

	var post_images_download = $("#post_images_download");
	post_images_download.empty(); //기존 내뇽 초기화

	if(imageUrls.length ===0) {
		post_images_download.hide();
		return;
	}
	var image_span =`
						<span id='image_downlad_span'>
							이미지: 
						</span>
					`;
	post_images_download.append(image_span);
    imageUrls.forEach(function(url) {
		//split(/\#|\?/) = 확장자 뒤에 오는 query(?)/hash(#)배열화 제거, [0]= 순수 이미지 이름 가져오기
        var fileName = url.split("/images/").pop().split(/\#|\?/)[0]; 
        var link = `<a href="${url}" download="${fileName}" style="margin: 0 5px;">${fileName}</a>`
        post_images_download.append(link);
    });

    post_images_download.show(); // 부모 영역 표시
}
//*****************************************Function End*************************************************************

//*****************************************API Start******************************************************************
// 메인, 부모 , 자식 게시글 공용으로 사용
function view_count_increment(postId) {
	console.log("main_post view_count_increment postId : ",postId);
	// ajax옵션 객체 셋팅
	$.ajax({
		url: `/posts/${postId}/view`,
		method: "PATCH",
		success: function(post) {
			getPostDetail(postId);
		},
		error: function(err) {
			console.log("조회수 증가 실패: " + err.responseText);
		}
	})
}

// 조회수 증가 API (메인, 부모 , 자식 게시글 공용으로 사용)
function view_count_increment(postId) {
	console.log("main_post view_count_increment postId : ",postId);
	// ajax옵션 객체 셋팅
	$.ajax({
		url: `/posts/${postId}/view`,
		method: "PATCH",
		success: function(post) {
			getPostDetail(postId);
		},
		error: function(err) {
			console.log("조회수 증가 실패: " + err.responseText);
		}
	})
}

// 좋아요/싫어요 클릭 API (메인, 부모 , 자식 게시글 공용으로 사용)
function reaction_api(postId,token) {
	if(!token) {
		if(confirm("로그인이 필요한 기능입니다. 로그인하시겠습니까?")) {
			window.location.href ="/signin";
		}
		return;	
	}
	$("#btn_like, #btn_dislike").off("click").on("click", function() {
		// 좋아요, 싫어요 버튼 클릭시 해당 객체 태그 ($(this)) 가져와서, 해당 태그의 'id' 삼항연산자로 비교
	    var reactionType = $(this).attr("id") === "btn_like" ? "LIKE" : "DISLIKE";

	    ajaxWithToken({
	        url: `/postreactions/${postId}/reaction`,
	        type: "POST",
	        contentType: "application/json",
	        data: JSON.stringify({ reactionType: reactionType }),
	        success: function(response) {
	            // 서버 응답 DTO(PostReactionResponseDTO) 값으로 UI 업데이트
	            $("#post_likes").text(response.likeCount);
	            $("#post_dislikes").text(response.dislikeCount);

	            // 내가 현재 누른 상태 강조 (LIKE, DISLIKE, null)
	            if (response.userPostReactionType === "LIKE") {
	                $("#btn_like").css("font-weight", "bold");
	                $("#btn_dislike").css("font-weight", "normal");
	            } else if (response.userPostReactionType === "DISLIKE") {
	                $("#btn_dislike").css("font-weight", "bold");
	                $("#btn_like").css("font-weight", "normal");
	            } else {
	                // 취소된 경우
	                $("#btn_like, #btn_dislike").css("font-weight", "normal");
	            }
	        },
	        error: function(xhr) {
	            alert("반응 처리 실패: " + xhr.responseText);
	        }
	    });
	});
}
//*****************************************API End******************************************************************