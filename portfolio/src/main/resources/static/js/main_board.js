// 타이머 변수 
var debounceTimer;

//*****************************************Board ID Start*************************************************************
// 도메인에서 '/'기준으로 배열화[] 5 -1  
//const pathParts = window.location.pathname.split("/");
// 도메인 배열에서 마지막 인덱스 boardId 가져오기
//const boardId = Number(pathParts[pathParts.lenth -1]);
// 공지게시판 BoardId
//const noticeBoard = [1];
// 부모게시판 BoardId
//const parentBoardIds = [9, 14, 15, 20];
//*****************************************Board ID End*************************************************************

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
//*****************************************Function End*************************************************************

//*****************************************API Start*************************************************************


//*****************************************API End*************************************************************

// 실행
$(document).ready(function() {
    autoCompleteSearchReset();
	autoCompleteReset();
});