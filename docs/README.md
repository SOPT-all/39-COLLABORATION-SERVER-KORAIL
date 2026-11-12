## 컨트롤러 응답 규칙
- 컨트롤러는 DTO만 반환합니다. (ApiResponse, ResponseEntity 사용 X)
- 모든 API에 `@Api{도메인}Success(코드)`를 붙입니다.
- 반환할 데이터가 없어도 `void` 대신 DTO를 반환합니다.
- `String`을 직접 반환하지 않습니다.

## 서비스 트랜잭션 규칙
- OSIV(`spring.jpa.open-in-view`)는 `false`로 둡니다.
  - DB 연결은 `@Transactional` 메서드 안에서만 유지됩니다. 컨트롤러에서는 LAZY 필드에 접근할 수 없습니다.
- 조회 메서드에는 `@Transactional(readOnly = true)`를, 변경 메서드에는 `@Transactional`을 붙입니다.
- 엔티티를 DTO로 바꾸는 작업은 **서비스의 트랜잭션 안에서** 끝냅니다. 컨트롤러로 엔티티를 넘기지 않습니다.
