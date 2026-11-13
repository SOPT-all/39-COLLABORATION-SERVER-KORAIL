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

## 엔티티 조회 후 예외 처리 규칙
- 엔티티를 조회하고 없으면 예외를 던지는 코드는 **해당 서비스의 private 메서드**로 둡니다.
- **여러 서비스가 같은 조회를 쓰면** 리포지토리의 default 메서드(`get~OrThrow`)로 올립니다.

```java
// 한 서비스에서만 사용
private Coach findCoach(Long coachId) {
    return coachRepository.findById(coachId)
            .orElseThrow(() -> new GeneralException(SeatErrorCode.COACH_NOT_FOUND));
}

// 여러 서비스에서 사용
default Schedule getByIdOrThrow(Long id) {
    return findById(id)
            .orElseThrow(() -> new GeneralException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));
}
```
- 리포지토리 메서드 이름은 `get~OrThrow` 형태로 씁니다.
- 리포지토리 default 메서드에 예외 처리를 두면 `@DataJpaTest`로 성공/실패 케이스를 테스트합니다.

## 엔티티 생성 규칙
- 객체 생성은 **필요한 필드만 받는 `private` 생성자에 `@Builder`** 를 붙여서 합니다.
- 기본 생성자는 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`로 둡니다.
- 생성자에는 `id`, 감사 필드(`createdAt`, `updatedAt`), 생성 시점에 값이 없는 필드를 넣지 않습니다.
- 필드를 추가하면 생성자 파라미터에도 추가합니다.

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Station {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private boolean major;

    @Builder
    private Station(String name, boolean major) {
        this.name = name;
        this.major = major;
    }
}

Station station = Station.builder()
        .name("서울")
        .major(true)
        .build();
```
