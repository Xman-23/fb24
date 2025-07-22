// ===================== 부모 게시판 필드 ===================== //

/**
 * 부모 게시판 엔티티.
 * 다대일(ManyToOne) 단방향 관계 설정.
 * 이 필드로 자식 게시판이 자신의 부모를 참조함.
 * DB 상에서는 'parent_id' 외래키(FK) 컬럼 생성됨.
 * → 자유게시판(1)이 부모일 경우, 유머게시판의 parent_id는 1
 */
@ManyToOne
@JoinColumn(name= "parent_id")
private Board parentBoard;


// ===================== 자식 게시판 리스트 필드 ===================== //

/**
 * 자식 게시판 리스트.
 * 일대다(OneToMany) 양방향 매핑의 역방향.
 * DB에는 컬럼이 생성되지 않으며, parentBoard 필드를 기준으로 역으로 매핑됨.
 * → 자유게시판.getChildBoards()로 유머게시판, 그냥게시판 등을 조회 가능.
 * 계층 구조(트리 구조) 조회 시 유용하게 사용됨.
 */
 
 
 
@OneToMany(mappedBy = "parentBoard")
private List<Board> childBoards = new ArrayList<>();

/* 
	계층 구조는 트리 형태의 관계이고, 리스트는 그 안에서 자식 노드 여러 개를 담는 자료구조다.
	@OneToMany(mappedBy = "parentBoard")와 private List<Board> childBoards 덕분에
	Board 엔티티에서 자신의 자식 게시판들을 리스트 형태로 갖고 있을 수 있다
	리스트를 DTO에서 그대로 필드로 두고 JSON으로 변환(예: @RestController에서 반환)하면, JSON 	라이브러리가 이 리스트를 배열 형태로 직렬화
*/
JSON 계층 구조

[
{
  "boardId": 1,
  "name": "자유게시판",
  "childBoards": [
    {
      "boardId": 2,
      "name": "유머게시판",
      "childBoards": [ ... ]
    },
    {
      "boardId": 3,
      "name": "정보게시판",
      "childBoards": []
    }
  ]
},
  {
    "boardId": 6,
    "name": "공지사항",
    "isActive": true,
    "sortOrder": 2,
    "childBoards": []
  }
]
