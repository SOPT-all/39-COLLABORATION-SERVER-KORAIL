# main 코드 컨벤션

`src/main` 코드 작성 규칙입니다. 테스트 코드는 [AI_TEST_GUIDE.md](./AI_TEST_GUIDE.md)를 따릅니다.

## 1. 패키지 구조

```
org.sopt.korail
├── global/          # 공통 응답·코드, 예외, 설정, 스웨거
└── {domain}/
    ├── code/        # {Domain}SuccessCode, {Domain}ErrorCode, @Api{Domain}Success, @Api{Domain}Errors
    ├── controller/  # {Domain}Api(스웨거 인터페이스), {Domain}Controller
    ├── domain/      # 엔티티, 도메인 enum
    ├── dto/         # request/, response/, 루트에는 쿼리 결과 등 내부용 DTO
    ├── repository/
    └── service/
```

- 여러 도메인이 쓰는 enum은 `global/enums`, 한 도메인만 쓰면 `{domain}/domain`에 둡니다.

## 2. Controller

스웨거 인터페이스(`{Domain}Api`)와 구현 클래스(`{Domain}Controller`)로 나눕니다.

```java
@Tag(name = "좌석 API", description = "좌석과 관련된 API 입니다")
@RequestMapping(path = "/api/v1/schedules/{scheduleId}/coaches")
public interface SeatApi {
    @Operation(summary = "호차 목록 조회", description = "...")
    @SecurityRequirements
    @GetMapping
    CoachListResponse getCoaches(
            @Parameter(description = "스케줄 ID", example = "1") @PathVariable Long scheduleId
    );
}

@RestController
@RequiredArgsConstructor
public class SeatController implements SeatApi {
    @Override
    @ApiSeatSuccess(SeatSuccessCode.COACH_LIST_OK)
    @ApiScheduleErrors({ScheduleErrorCode.SCHEDULE_NOT_FOUND})
    public CoachListResponse getCoaches(Long scheduleId) {
        return seatService.getCoaches(scheduleId);
    }
}
```

- 인터페이스: 매핑, 스웨거 문서(`@Operation`, `@Parameter`), 파라미터 어노테이션
- 구현 클래스: `@Api{Domain}Success`, `@Api{Domain}Errors`
- 인증(`Member-Id` 헤더)이 필요 없는 API는 `@SecurityRequirements`를 붙입니다.
- URL은 `/api/v1/` + 복수형 리소스입니다.

### 응답 규칙
- 컨트롤러는 DTO만 반환합니다. `ApiResponse` 래핑은 `ApiResponseAdvice`가 합니다. (`ApiResponse`, `ResponseEntity`, `String`, `void` 반환 X)
- 모든 API에 `@Api{Domain}Success`를 붙입니다.
- 발생할 수 있는 에러 코드를 `@Api{Domain}Errors`로 모두 붙입니다. 다른 도메인의 에러는 그 도메인의 어노테이션을 씁니다.

## 3. 응답 코드

```java
@Getter
@RequiredArgsConstructor
public enum SeatErrorCode implements BaseErrorCode {
    COACH_NOT_FOUND(HttpStatus.NOT_FOUND, "COACH4041", "존재하지 않는 호차입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
```

- 성공 코드는 `BaseSuccessCode`, 에러 코드는 `BaseErrorCode`를 구현합니다.
- 코드 문자열: `{대상}{HTTP 상태}{순번}` (`SEAT2001`, `COACH4041`)
- 상수 이름: `{대상}_{상황}` (`COACH_LIST_OK`, `COACH_NOT_FOUND`)
- 코드 enum을 만들면 짝이 되는 스웨거 어노테이션도 만듭니다. 마커가 있어야 스웨거에 응답 예시가 나옵니다.

```java
@SuccessCodeMarker
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiSeatSuccess {
    SeatSuccessCode value();
}

@ErrorCodeMarker
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiSeatErrors {
    SeatErrorCode[] value();
}
```

## 4. Service

### 트랜잭션
- 클래스에 `@Transactional(readOnly = true)`, 변경 메서드에만 `@Transactional`을 붙입니다.
- OSIV가 꺼져 있습니다. 엔티티 → DTO 변환은 서비스 안에서 끝내고, 컨트롤러로 엔티티를 넘기지 않습니다.
- 현재 시간이 필요한 쿼리는 서비스에서 `LocalDateTime.now()`를 만들어 인자로 넘깁니다.

### 조회 후 예외 처리
- 한 서비스에서만 쓰면 서비스의 private 메서드로 둡니다.
- 여러 서비스가 쓰면 리포지토리 default 메서드 `get~OrThrow`로 올립니다.

```java
private Coach findCoach(Long coachId) {
    return coachRepository.findById(coachId)
            .orElseThrow(() -> new GeneralException(SeatErrorCode.COACH_NOT_FOUND));
}

default Schedule getByIdOrThrow(Long id) {
    return findById(id)
            .orElseThrow(() -> new GeneralException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));
}
```

### 예외
- 비즈니스 예외는 `GeneralException({Domain}ErrorCode)`로만 던집니다. 새 예외 클래스를 만들지 않습니다.

## 5. Repository

- 복잡한 조건·정렬·집계는 `@Query` + 텍스트 블록(`"""`) JPQL로 씁니다.
- 엔티티가 아닌 결과(집계 등)는 `new` 생성자 표현식으로 `dto` 패키지의 record에 담습니다.

## 6. Entity

- `@Getter`만 둡니다. (`@Setter`, `@Data`, `@AllArgsConstructor` X)
- 연관관계는 `@ManyToOne(fetch = FetchType.LAZY)`로 둡니다.
- enum 필드는 `@Enumerated(EnumType.STRING)`을 붙입니다.
- 생성·수정 시간이 필요하면 `BaseTimeEntity`를 상속합니다.

### 생성
- 기본 생성자는 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`로 둡니다.
- 필요한 필드만 받는 `private` 생성자에 `@Builder`를 붙입니다.
- 생성자에 `id`, `createdAt`/`updatedAt`, 생성 시점에 값이 없는 필드는 넣지 않습니다.

```java
@Builder
private Seat(Coach coach, Direction direction, int seatRow, String seatCol) { ... }
```

## 7. DTO

- 이름은 `{기능}Request` / `{기능}Response`로 짓습니다. (`SeatLayoutResponse`)
- `record`로 작성하고, 요청/응답 DTO의 모든 필드에 `@Schema(description, example)`를 붙입니다.
- 엔티티 하나를 옮기면 `static of(...)`, 여러 값을 조합하면 `@Builder`로 만듭니다.
- 응답 DTO에 엔티티를 직접 담지 않습니다.
- 요청 DTO는 Bean Validation으로 검증하고 컨트롤러 파라미터에 `@Valid`를 붙입니다.
