# Page<T> vs Stream<T> 비교 정리

| 구분          | Page<T>                      | Stream<T>                       |
|---------------|------------------------------|--------------------------------|
| **비유**      | 완성된 책 한 권               | 책 읽는 ‘과정’ (아직 완성 안 됨)  |
| **상태**      | 데이터 + 메타정보 완성 상태    | 처리 과정, 지연 실행 상태          |
| **map() 사용**| 바로 `map()`으로 내부 데이터 변환 가능 | `map()`은 변환 정의만, 최종 결과는 `collect()`로 만들어야 함 |
| **최종 결과 획득** | `Page<R>`로 즉시 반환           | `collect()`로 List, Set 등 결과 생성 |

---

## 설명

- `Page`는 내부에 이미 데이터(`List<T> content`)가 있어서 **즉시 변환하고 반환 가능**  
- `Stream`은 **처리 과정(중간 연산)만 정의**하고,  
  **최종 연산(`collect()`, `forEach()`)이 실행돼야 결과가 나옴**

---

## 코드 예시

```java
// Page 사용
Page<Post> postsPage = postRepository.findAll(pageable);
Page<PostDTO> dtoPage = postsPage.map(PostDTO::new);  // 즉시 변환

// Stream 사용
List<Post> postsList = postRepository.findAll();
List<PostDTO> dtoList = postsList.stream()
                                .map(PostDTO::new)
                                .collect(Collectors.toList());  // 최종 결과 생성
