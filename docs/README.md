## 컨트롤러 응답 규칙
- 컨트롤러는 DTO만 반환합니다. (ApiResponse, ResponseEntity 사용 X)
- 모든 API에 `@Api{도메인}Success(코드)`를 붙입니다.
- 반환할 데이터가 없어도 `void` 대신 DTO를 반환합니다.
- `String`을 직접 반환하지 않습니다.

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
