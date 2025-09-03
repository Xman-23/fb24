// 타이머 변수 
var debounceTimer;
var token = localStorage.getItem('accessToken'); // 현재 액세스 토큰 가져오기

//*****************************************Board ID Start*************************************************************
// 도메인에서 '/'기준으로 배열화[] 5 -1  
const pathParts = window.location.pathname.split("/");
console.log("pathParts:", pathParts);
// 도메인 배열에서 2번째 인덱스 boardId 가져오기
const boardId = Number(pathParts[pathParts.length - 3]);
// 도메인에서 마지막 인덱스 postId 가져오기
const postId = Number(pathParts[pathParts.length - 1]);
// 공지게시판 BoardId
const noticeBoard = [1];
// 부모게시판 BoardId
const parentBoardIds = [9, 14, 15, 20];

var memberId = Number(localStorage.getItem('memberId'));
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

// 이미지 영역(post_images_download)보여주기 메인, 부모 , 자식 게시글 공용으로 사용
function shwo_image_download(imageUrls) {

	$("#post_images_download").empty(); //기존 내뇽 초기화

	if(imageUrls.length ===0) {
		$("#post_images_download").hide();
		return;
	}
	var image_span =`
						<span id='image_downlad_span'>
							이미지: 
						</span>
					`;
	$("#post_images_download").append(image_span);
    imageUrls.forEach(function(url) {
		//split(/\#|\?/) = 확장자 뒤에 오는 query(?)/hash(#)배열화 제거, [0]= 순수 이미지 이름 가져오기
        var fileName = url.split("/images/").pop().split(/\#|\?/)[0]; 
        var link = `<a href="${url}" download="${fileName}" style="margin: 0 5px;">${fileName}</a>`
        $("#post_images_download").append(link);
    });

    $("#post_images_download").show(); // 부모 영역 표시
}
//*****************************************Board Function End*************************************************************
//*****************************************Function End*************************************************************

//*****************************************API Start******************************************************************
//*****************************************Post API Start******************************************************************
// 조회수 증가 API (메인, 부모 , 자식 게시글 공용으로 사용)
function view_count_increment(postId) {
	// ajax옵션 객체 셋팅
	$.ajax({
		url: `/posts/${postId}/view`,
		method: "PATCH",
		success: function() {
			getPostDetail(postId);
		},
		error: function(err) {
			console.log("조회수 증가 실패: " + err.responseText);
		}
	})
}

// 게시글 호출 API
function getPostDetail() {
	$("#post_title").empty();
	$("#post_board").empty();
	$("#post_author").empty();
	$("#post_created").empty();
	$("#post_views").empty();
	$("#post_likes").empty();
	$("#post-dislikes").empty();
	$("#post_content").empty();
	$("#post_images").empty();
	$("#post_images_download").empty();
	
	// ajax옵션 객체 셋팅
	$.ajax({
		url: `/posts/${postId}`,
		method: 'GET',
		success: function(post) {
			if(post) {
				renderPost(post);
			}else {
				return;
			}
		},
		error: function(err) {
			console.log("게시글 조회 실패: " + err.responseText);
		}
	})
}

// 상세 게시글 좋아요/싫어요 클릭 API (메인, 부모 , 자식 게시글 공용으로 사용)
function reaction_api(postId,token) {

	$("#post_btn_like, #post_btn_dislike").off("click").on("click", function() {

		if(!token) {
			if(confirm("로그인이 필요한 기능입니다. 로그인하시겠습니까?")) {
				window.location.href ="/signin";
			}
			return;	
		}

		// 좋아요, 싫어요 버튼 클릭시 해당 객체 태그 ($(this)) 가져와서, 해당 태그의 'id' 삼항연산자로 비교
	    var reactionType = $(this).attr("id") === "post_btn_like" ? "LIKE" : "DISLIKE";

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
	                $("#post_btn_like").css("font-weight", "bold");
	                $("#post_btn_dislike").css("font-weight", "normal");
	            } else if (response.userPostReactionType === "DISLIKE") {
	                $("#post_btn_dislike").css("font-weight", "bold");
	                $("#post_btn_like").css("font-weight", "normal");
	            } else {
	                // 취소된 경우
	                $("#post_btn_like, #post_btn_dislike").css("font-weight", "normal");
	            }
	        },
	        error: function(xhr) {
	            alert(xhr.responseText);
	        }
	    });
	});
}
//*****************************************Post API End******************************************************************
//*************************************************** Post 랜더링 Start ***************************************************//

function renderPost(post) {
	$("#post_title").text("[HOT🔥] " + post.title);
	$("#post_board").text(post.boardName);
	$("#post_views").text(post.viewCount);
	$("#post_author").text(post.userNickname);
	$("#post_created").text(post.createdAt);
	$("#post_likes").text(post.likeCount);
	$("#post_dislikes").text(post.dislikeCount);
	$("#post_content").html(post.content);

    if (post.imageUrls && post.imageUrls.length > 0) {
        post.imageUrls.forEach(url => {
        	$("#post_images").append(`<img src="${url}" alt="게시글 이미지">`);
        });
        shwo_image_download(post.imageUrls);
    }
}

//*************************************************** Post 랜더링 End ***************************************************//

//*****************************************API End******************************************************************
$(document).ready(function() {
	/* Post API Start */
	view_count_increment(postId); //조회수 증가(그안에 게시글 불러오기)
	reaction_api(postId,token); // 게시글 리액션
	/* Post API End */

});