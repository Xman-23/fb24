# Spring Data JPA 메서드 네이밍 규칙 및 예시

Spring Data JPA는 메서드 이름을 해석하여 자동으로 쿼리를 생성합니다.  
아래는 주요 접두어와 조건 키워드별 예시입니다.

---

## 1. 주요 접두어 (메서드 동작 유형)

| 접두어       | 역할                  | 설명                         | 예시 코드                           |
|--------------|-----------------------|------------------------------|-----------------------------------|
| `findBy`     | 조회 (SELECT)          | 조건에 맞는 엔티티 리스트 반환 | `List<User> findByUsername(String username);` |
| `getBy`      | 조회 (SELECT)          | `findBy`와 동일 기능          | `User getByEmail(String email);`  |
| `readBy`     | 조회 (SELECT)          | `findBy`와 동일 기능          | `List<Post> readByStatus(PostStatus status);` |
| `countBy`    | 개수 세기 (SELECT COUNT) | 조건에 맞는 개수 반환          | `long countByStatus(PostStatus status);`       |
| `existsBy`   | 존재 여부 확인          | 조건에 맞는 데이터 존재 여부 반환 (boolean) | `boolean existsByEmail(String email);`          |
| `deleteBy`   | 삭제 (DELETE)          | 조건에 맞는 데이터 삭제       | `void deleteByStatus(PostStatus status);`       |
| `removeBy`   | 삭제 (DELETE)          | `deleteBy`와 동일 기능        | `void removeByExpired(boolean expired);`        |

---

## 2. 조건 키워드 (필드 연결 및 조건)

| 키워드         | 역할                         | 예시 메서드 이름                                     | 설명                                      |
|----------------|------------------------------|----------------------------------------------------|-------------------------------------------|
| `And`          | AND 조건 연결                 | `findByStatusAndAuthorId(PostStatus status, Long authorId);` | status = ? AND authorId = ?                |
| `Or`           | OR 조건 연결                 | `findByStatusOrTitle(PostStatus status, String title);` | status = ? OR title = ?                     |
| `Between`      | 범위 조건                   | `findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);` | createdAt BETWEEN ? AND ?                  |
| `LessThan`     | 작음 (<)                   | `findByAgeLessThan(int age);`                       | age < ?                                    |
| `LessThanEqual`| 작거나 같음 (<=)            | `findByPriceLessThanEqual(BigDecimal price);`       | price <= ?                                 |
| `GreaterThan`  | 큼 (>)                    | `findBySalaryGreaterThan(int salary);`              | salary > ?                                 |
| `GreaterThanEqual`| 크거나 같음 (>=)         | `findByScoreGreaterThanEqual(int score);`           | score >= ?                                 |
| `Like`         | LIKE 검색                   | `findByTitleLike(String pattern);`                   | title LIKE ?                               |
| `StartingWith` | 시작 문자열 검색            | `findByUsernameStartingWith(String prefix);`        | username LIKE 'prefix%'                     |
| `EndingWith`   | 끝 문자열 검색             | `findByEmailEndingWith(String suffix);`              | email LIKE '%suffix'                        |
| `Containing`   | 포함 (부분 일치)           | `findByContentContaining(String keyword);`           | content LIKE '%keyword%'                    |
| `In`           | 컬렉션 내 포함 여부          | `findByStatusIn(List<PostStatus> statuses);`          | status IN (?, ?, ...)                       |
| `OrderBy`      | 정렬                         | `findByStatusOrderByCreatedAtDesc(PostStatus status);`| status = ? ORDER BY createdAt DESC         |

---

## 3. 상세 예시 코드

```java
// 1) 기본 조회
List<User> findByUsername(String username); // username이 일치하는 유저 리스트 조회

// 2) AND 조건
List<Post> findByStatusAndAuthorId(PostStatus status, Long authorId); // status, authorId 모두 일치하는 게시글 조회

// 3) OR 조건
List<Post> findByStatusOrTitle(PostStatus status, String title); // status 또는 title 중 하나라도 일치하는 게시글 조회

// 4) Between (범위)
List<Order> findByOrderDateBetween(LocalDate start, LocalDate end); // 주문일자가 start와 end 사이인 주문 조회

// 5) LessThan, LessThanEqual
List<Product> findByPriceLessThan(BigDecimal price); // price보다 작은 상품 조회
List<Product> findByPriceLessThanEqual(BigDecimal price); // price 이하 상품 조회

// 6) GreaterThan, GreaterThanEqual
List<Employee> findBySalaryGreaterThan(int salary); // salary보다 큰 직원 조회
List<Employee> findBySalaryGreaterThanEqual(int salary); // salary 이상 직원 조회

// 7) Like
List<Article> findByTitleLike(String pattern); // 제목이 pattern과 유사한 글 조회 (ex: "%Spring%")

// 8) StartingWith
List<User> findByUsernameStartingWith(String prefix); // username이 prefix로 시작하는 유저 조회

// 9) EndingWith
List<User> findByEmailEndingWith(String domain); // email이 domain으로 끝나는 유저 조회

// 10) Containing
List<Post> findByContentContaining(String keyword); // content에 keyword가 포함된 게시글 조회

// 11) In
List<Post> findByStatusIn(List<PostStatus> statuses); // 여러 상태 중 하나에 해당하는 게시글 조회

// 12) OrderBy
List<Post> findByStatusOrderByCreatedAtDesc(PostStatus status); // status 조건 만족하며 최신순 정렬
