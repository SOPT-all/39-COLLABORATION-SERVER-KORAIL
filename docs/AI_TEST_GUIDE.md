# 테스트 코드 작성 지침 (AI용)

이 문서는 AI가 이 프로젝트의 테스트 코드를 작성할 때 따르는 규칙입니다.
규칙과 다르게 작성해야 하면 임의로 바꾸지 말고 먼저 사람에게 확인합니다.

## 0. 프로젝트 전제

- Spring Boot 4, JUnit 5(Jupiter), Mockito, AssertJ를 사용합니다.
- Jackson 3을 사용합니다. `ObjectMapper`는 `tools.jackson.databind.ObjectMapper`를 import합니다. (`com.fasterxml.jackson.databind` 아님)
- `@WebMvcTest`는 `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`를 import합니다. (Boot 4에서 패키지 변경)
- Mock 빈은 `@MockitoBean`을 사용합니다. (`@MockBean`은 사용하지 않음)
- 성공 응답은 `ApiResponseAdvice`가 `ApiResponse`로 자동 래핑합니다. 응답 JSON 형태는 `{ isSuccess, code, message, result }`입니다.
- 서비스 예외는 `GeneralException(BaseErrorCode)`로 던지고, `GlobalExceptionHandler`가 에러 응답으로 바꿉니다.

## 1. 무엇을 테스트하나

| 대상 | 방식 | 필수 여부 |
|---|---|---|
| Service | Mockito 단위 테스트 | 필수 |
| Controller | `@WebMvcTest` 슬라이스 테스트 | 필수 |
| 엔티티 도메인 메서드 (상태 변경 등) | 순수 단위 테스트 | 로직이 있을 때만 |
| Repository 커스텀 쿼리 | `@DataJpaTest` | 사람에게 확인 후 작성 |
| Repository `get~OrThrow` default 메서드 | `@DataJpaTest` (성공/실패) | 필수 |

테스트하지 않는 것:
- getter, 빌더, DTO, `global/config`, `global/swagger`
- 로직 없이 Repository를 그대로 호출만 하는 코드

## 2. 파일 위치와 이름

- 테스트 클래스는 main 코드와 **같은 패키지**에 둡니다.
  - `src/main/java/org/sopt/korail/reservation/service/ReservationService.java`
  - → `src/test/java/org/sopt/korail/reservation/service/ReservationServiceTest.java`
- 클래스 이름은 `{대상클래스}Test`입니다.
- 픽스처는 `src/test/java/org/sopt/korail/support/fixture/{Domain}Fixture.java`에 둡니다.

## 3. 클래스 구조

모든 테스트는 **기능 → 성공/실패 → 테스트 케이스** 3단계 `@Nested` 구조로 작성합니다.

```java
@DisplayName("ReservationService 단위 테스트")
class ReservationServiceTest {

    @Nested
    @DisplayName("예약 생성")                       // 1단계: 기능 (메서드 1개 = Nested 1개)
    class CreateReservation {

        @Nested
        @DisplayName("성공")                         // 2단계: 성공 / 실패
        class Success {

            @Test
            @DisplayName("빈 좌석이면 PENDING 상태로 예약을 생성한다")   // 3단계: 케이스
            void 빈좌석_예약생성성공() { ... }
        }

        @Nested
        @DisplayName("실패")
        class Failure { ... }
    }
}
```

- 1단계 `@Nested` 클래스 이름은 테스트 대상 메서드 이름을 PascalCase로 씁니다. (`createReservation` → `CreateReservation`)
- 성공/실패 클래스 이름은 항상 `Success`, `Failure`입니다.
- 케이스가 1~2개뿐인 단순 조회 메서드는 2단계(성공/실패)를 생략해도 됩니다.

## 4. 이름 규칙

| 대상 | 규칙 | 예시 |
|---|---|---|
| 최상위 `@DisplayName` | `{클래스명} 단위 테스트` / `{클래스명} 테스트` | `ReservationService 단위 테스트` |
| 기능 `@DisplayName` | 한국어 기능명. 필요하면 `(메서드명)` 병기 | `예약 상세 조회(getReservation)` |
| Controller 기능 `@DisplayName` | `{HTTP 메서드} {경로} - {기능명}` | `GET /api/v1/reservations/{id} - 예약 상세 조회` |
| 케이스 `@DisplayName` | **"~하면 ~한다"** 형태의 완전한 문장 | `만료된 예약이면 RESERVATION_EXPIRED를 던진다` |
| 테스트 메서드명 | 한국어, `조건_결과` 형태, 밑줄로 구분 | `만료된예약_예외`, `좌석없음_404를_반환한다` |

- 실패 케이스의 `@DisplayName`에는 **던지는 에러 코드 이름**을 적습니다.
- 같은 클래스 안에서 메서드명이 겹치지 않게 합니다. (예: `생성시_회원없음_예외`, `조회시_회원없음_예외`)

## 5. 테스트 본문 작성법

given / when / then 순서로 쓰고, **주석 없이 빈 줄로 구분**합니다.

```java
@Test
@DisplayName("빈 좌석이면 PENDING 상태로 예약을 생성한다")
void 빈좌석_예약생성성공() {
    Member member = member();
    given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
    given(reservationRepository.save(any(Reservation.class))).willAnswer(inv -> inv.getArgument(0));

    ReservationResponse result = reservationService.createReservation(MEMBER_ID, request());

    assertThat(result.status()).isEqualTo(ReservationStatus.PENDING);
    verify(reservationRepository).save(any());
}
```

- 스텁은 `BDDMockito.given(...).willReturn(...)` / `willThrow(...)`를 사용합니다. (`when(...).thenReturn(...)` 사용 X)
- 검증은 AssertJ `assertThat`을 사용합니다. (JUnit `assertEquals` 사용 X)
- 저장/삭제처럼 **반환값이 없는 부수 효과**는 `verify`로 검증합니다.
- 호출되면 안 되는 경우는 `verify(mock, never())`로 검증합니다.
- 한 테스트는 한 가지 상황만 검증합니다.
- 구현 세부사항을 모두 검증하지 않습니다. **결과, 상태 변화, 외부 협력 객체와의 의미 있는 상호작용**만 검증합니다. (모든 Mock 호출에 `verify`를 붙이지 않음)

### void 메서드 스텁

void 메서드는 `given(...)`을 쓸 수 없으므로 BDD 형식의 `willThrow(...).given(mock).메서드()`를 사용합니다. (`doThrow(...).when(...)` 사용 X)

```java
willThrow(new GeneralException(ReservationErrorCode.RESERVATION_EXPIRED))
        .given(reservationService).cancelReservation(MEMBER_ID, RESERVATION_ID);
```

- 아무것도 하지 않는 void 메서드는 Mock 기본 동작이므로 `willDoNothing()`을 따로 쓰지 않습니다.

### save 스텁과 저장 값 검증

- `save`는 받은 객체를 그대로 돌려주도록 스텁합니다. 반환용 픽스처를 따로 만들지 않습니다.
  ```java
  given(reservationRepository.save(any(Reservation.class))).willAnswer(inv -> inv.getArgument(0));
  ```
- Mock은 실제 DB처럼 `id`를 채워주지 않으므로, 위 방식으로 돌려받은 객체의 `id`는 `null`입니다.
  서비스가 저장된 객체의 `id`를 사용하면(응답 DTO에 담는 등) `willAnswer` 안에서 `id`를 넣고 돌려줍니다.
  ```java
  given(reservationRepository.save(any(Reservation.class))).willAnswer(inv -> {
      Reservation saved = inv.getArgument(0);
      ReflectionTestUtils.setField(saved, "id", RESERVATION_ID);
      return saved;
  });
  ```
- 서비스가 **만들어서 저장한 객체의 값**을 확인해야 하면 `ArgumentCaptor`로 꺼내서 검증합니다.
  ```java
  ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
  verify(reservationRepository).save(captor.capture());
  assertThat(captor.getValue().getStatus()).isEqualTo(ReservationStatus.PENDING);
  ```

## 6. Service 단위 테스트

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationService 단위 테스트")
class ReservationServiceTest {
    @InjectMocks private ReservationService reservationService;

    @Mock private MemberRepository memberRepository;
    @Mock private ReservationRepository reservationRepository;
    ...
}
```

- 서비스가 인터페이스와 구현체로 나뉘어 있으면 `@InjectMocks`에는 **구현체**를 선언합니다.

### 예외 검증 형식 (고정)

```java
assertThatThrownBy(() -> reservationService.getReservation(MEMBER_ID, RESERVATION_ID))
        .isInstanceOf(GeneralException.class)
        .satisfies(ex -> assertThat(((GeneralException) ex).getErrorCode())
                .isEqualTo(ReservationErrorCode.RESERVATION_NOT_FOUND));
```

- 예외 타입만 확인하지 말고 **에러 코드까지** 확인합니다.

## 7. Controller 슬라이스 테스트

```java
@WebMvcTest(ReservationController.class)
@DisplayName("ReservationController 테스트")
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReservationService reservationService;
}
```

- 컨트롤러가 의존하는 빈은 모두 `@MockitoBean`으로 대체합니다.
- `ApiResponseAdvice`, `GlobalExceptionHandler`는 `@RestControllerAdvice`라서 `@WebMvcTest`에 자동 포함됩니다. `@Import`하지 않습니다.

### 성공 케이스: 상태 코드 → 공통 응답 → result 내용 순서로 검증

```java
mockMvc.perform(get("/api/v1/reservations/{id}", RESERVATION_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isSuccess").value(true))
        .andExpect(jsonPath("$.code").value(CommonSuccessCode.OK.getCode()))
        .andExpect(jsonPath("$.result.id").value(RESERVATION_ID))
        .andExpect(jsonPath("$.result.status").value("PENDING"));
```

- `result` 내용은 `jsonPath("$.result.필드명")`으로 검증합니다. 응답 JSON을 DTO로 역직렬화해서 검증하지 않습니다. (테스트 때문에 응답 DTO에 기본 생성자 등 역직렬화 조건이 강제되는 것을 막기 위함)
- 목록 응답은 `jsonPath("$.result.length()").value(2)`, `jsonPath("$.result[0].id").value(...)`처럼 검증합니다.
- enum 필드는 JSON에 이름 문자열로 담기므로 `"PENDING"`처럼 문자열로 비교합니다.

### 실패 케이스: 서비스가 예외를 던지게 하고 에러 응답을 검증

```java
given(reservationService.getReservation(RESERVATION_ID))
        .willThrow(new GeneralException(ReservationErrorCode.RESERVATION_NOT_FOUND));

mockMvc.perform(get("/api/v1/reservations/{id}", RESERVATION_ID))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.isSuccess").value(false))
        .andExpect(jsonPath("$.code").value(ReservationErrorCode.RESERVATION_NOT_FOUND.getCode()));
```

- 응답 코드 문자열은 하드코딩하지 않고 `{Code}.getCode()`로 참조합니다. 성공 코드도 마찬가지입니다. (`"COMMON200"` X → `CommonSuccessCode.OK.getCode()`)
- 요청 검증(`@Valid`) 실패 케이스는 `status().isBadRequest()`, `isSuccess=false`, `$.code`가 `CommonErrorCode.BAD_REQUEST.getCode()`인지 검증합니다.

### 요청 본문이 있는 API (POST / PATCH / PUT)

- 요청에 `.contentType(MediaType.APPLICATION_JSON)`과 `.content(objectMapper.writeValueAsString(request))`를 붙입니다.
- JSON 문자열을 직접 하드코딩하지 않고, 요청 DTO 객체를 만들어 직렬화합니다.
- 서비스 스텁의 요청 인자는 `any(요청DTO.class)`로 받습니다. 요청 내용 자체를 검증해야 할 때만 `ArgumentCaptor`를 사용합니다.
- 생성 API는 성공 코드가 `COMMON201`이면 `status().isCreated()`와 `$.code`를 함께 검증합니다.
- `@Valid` 실패 케이스는 **제약조건마다** 1개 작성합니다. (한 필드에 `@NotNull`, `@Positive`가 함께 있으면 각각 1개)

## 8. 픽스처

- 테스트 데이터는 `support/fixture/{Domain}Fixture`에 **상수 + static 팩토리 메서드**로 모읍니다.
- 테스트 클래스에서는 `import static ...ReservationFixture.*;`로 가져다 씁니다.
- 테스트 안에서 같은 객체를 여러 번 직접 빌드하지 않습니다. 반복되면 픽스처로 옮깁니다.
- 픽스처를 지나치게 범용화하지 않습니다. `member()`, `member(id)`처럼 작은 변형만 둡니다.
- **테스트 결과를 좌우하는 값은 테스트 코드에 드러나게** 합니다. 픽스처 이름 속에 조건을 숨기지 않고 인자로 넘깁니다.
  (`expiredReservation()` X → `reservation(member, PAST_TIME)` O)

```java
public final class MemberFixture {
    public static final Long MEMBER_ID = 1L;
    public static final Long OTHER_MEMBER_ID = 2L;

    private MemberFixture() {
    }

    public static Member member() {
        return member(MEMBER_ID);
    }

    public static Member member(Long id) {
        Member member = BeanUtils.instantiateClass(Member.class);
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}

public final class StationFixture {
    public static final Long STATION_ID = 1L;

    private StationFixture() {
    }

    public static Station station() {
        Station station = Station.builder()
                .name("서울")
                .major(true)
                .build();
        ReflectionTestUtils.setField(station, "id", STATION_ID);
        return station;
    }
}
```

- 빌더가 있는 엔티티는 **빌더로 생성**합니다.
- 빌더가 없는 엔티티(현재 `Member`처럼 `id` 외 필드가 없는 경우)는 `BeanUtils.instantiateClass(엔티티.class)`로 생성합니다. (`org.springframework.beans.BeanUtils`, `protected` 기본 생성자도 호출 가능)
- 엔티티 빌더에는 `id`가 없으므로 `ReflectionTestUtils.setField(entity, "id", id)`로 설정합니다.
- 픽스처를 만들려고 main 코드에 `public` 생성자, 빌더, `id` 파라미터, setter를 추가하지 않습니다.

## 9. 하지 말 것

- 단위 테스트에 `@SpringBootTest` 사용
- 단위 테스트에서 실제 DB, 외부 API에 의존
- `Thread.sleep` 사용
- 테스트 간 순서 의존 (`@TestMethodOrder` 등)
- 테스트를 통과시키려고 main 코드의 접근 제어자나 구조를 바꾸는 것
- 실패하는 테스트를 `@Disabled`로 숨기는 것 (필요하면 사유를 사람에게 먼저 알림)

## 10. 작성 후 확인

1. `./gradlew test`가 통과하는지 확인합니다.
2. 성공/실패 케이스가 모두 있는지 확인합니다. **서로 다른 비즈니스 분기·검증 규칙마다** 실패 케이스가 1개 이상 있어야 합니다.
   - 같은 에러 코드라도 서로 다른 분기에서 발생하면 각각 테스트합니다.
   - 입력값만 다르고 같은 분기를 타는 경우는 대표 케이스 1개만 작성합니다.
