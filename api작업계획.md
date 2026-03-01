# 감정 일기장 API 서버 작업계획

> **목적**: React 프론트엔드(PRD 5장 API 명세)에 맞는 Spring Boot API 서버 구축
> **스택**: Java 17+ / Spring Boot 3.x / MySQL 8 / JPA / 인증 없음

---

## 1. 사전 검토 사항

### 1.1 프론트엔드 API 명세 확인 완료

PRD 5장 기준 5개 엔드포인트:

| Method | Path | 설명 | 응답코드 |
|--------|------|------|----------|
| GET | `/api/diaries?from=&to=&sort=` | 월별 목록 조회 | 200 |
| GET | `/api/diaries/{id}` | 단건 조회 | 200 / 404 |
| POST | `/api/diaries` | 생성 | 201 |
| PUT | `/api/diaries/{id}` | 수정 | 200 / 404 |
| DELETE | `/api/diaries/{id}` | 삭제 | 204 / 404 |

### 1.2 추가 확인/결정 필요 사항

| 항목 | 결정 | 근거 |
|------|------|------|
| **ID 전략** | UUID (String) | PRD 응답 예시가 `"uuid-1"`, `"uuid-new"` 형태. 프론트 타입도 `string \| number` |
| **Emotion 테이블 필요 여부** | 불필요 | 감정은 1~5 고정 상수. diary 테이블의 `emotion_id` 컬럼으로 충분. 유효성은 서버 검증만 수행 |
| **날짜 저장 형식** | `BIGINT` (timestamp ms) | PRD 규칙: "날짜는 timestamp(ms)로 송수신". 변환 없이 그대로 저장하면 프론트와 일치 |
| **CORS 설정** | 필수 | 프론트(Vite, 5173 포트)와 백엔드(8080 포트) 도메인이 다름. 전체 허용 또는 특정 origin 허용 |
| **정렬 파라미터** | `latest` / `oldest` 문자열 | PRD 명세 그대로. 서버에서 `date DESC` / `date ASC`로 변환 |
| **content 최대 길이** | 2000자 | PRD 검증 규칙. DB 컬럼은 `TEXT` 또는 `VARCHAR(2000)` |

---

## 2. 데이터베이스 설계

### 2.1 ERD

테이블 1개 (diary)

```
┌─────────────────────────────────┐
│            diary                │
├─────────────────────────────────┤
│ id          VARCHAR(36) PK      │  ← UUID
│ date        BIGINT     NOT NULL │  ← timestamp(ms)
│ content     VARCHAR(2000) NOT NULL │
│ emotion_id  INT        NOT NULL │  ← 1~5
│ created_at  DATETIME   NOT NULL │  ← 서버 관리용
│ updated_at  DATETIME   NOT NULL │  ← 서버 관리용
├─────────────────────────────────┤
│ INDEX idx_date (date)           │  ← 월별 조회 성능
│ CHECK (emotion_id BETWEEN 1 AND 5) │
└─────────────────────────────────┘
```

### 2.2 DDL

```sql
CREATE DATABASE IF NOT EXISTS emotion_diary
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE emotion_diary;

CREATE TABLE diary (
    id          VARCHAR(36)   NOT NULL PRIMARY KEY,
    date        BIGINT        NOT NULL COMMENT 'Unix timestamp(ms)',
    content     VARCHAR(2000) NOT NULL,
    emotion_id  INT           NOT NULL,
    created_at  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_emotion_id CHECK (emotion_id BETWEEN 1 AND 5),
    INDEX idx_date (date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 2.3 설계 결정 근거

- **테이블 1개**: 인증 없이 단일 사용자 전제. user 테이블 불필요
- **UUID PK**: JPA `@GeneratedValue`로 UUID 자동 생성. Auto Increment보다 프론트 명세에 부합
- **date를 BIGINT로 저장**: 프론트가 timestamp(ms)로 송수신하므로 변환 로직 없이 그대로 저장/응답
- **created_at / updated_at**: API 응답에는 포함하지 않지만 운영/디버깅용으로 관리
- **idx_date 인덱스**: `from <= date <= to` 범위 조회에 사용

---

## 3. 프로젝트 구조

```
src/main/java/it/codro/emotiondiary/
├── EmotionDiaryApplication.java
├── config/
│   ├── CorsConfig.java              # CORS 전역 설정
│   └── WebConfig.java               # 기타 웹 설정 (필요시)
├── controller/
│   └── DiaryController.java         # REST 엔드포인트
├── service/
│   └── DiaryService.java            # 비즈니스 로직
├── repository/
│   └── DiaryRepository.java         # JPA Repository
├── entity/
│   └── Diary.java                   # JPA Entity
├── dto/
│   ├── DiaryRequest.java            # 생성/수정 요청 DTO
│   ├── DiaryResponse.java           # 단건 응답 DTO
│   └── DiaryListResponse.java       # 목록 응답 DTO (items + total)
└── exception/
    ├── GlobalExceptionHandler.java  # @ControllerAdvice
    ├── DiaryNotFoundException.java  # 404 예외
    └── ErrorResponse.java           # 에러 응답 DTO (code + message)
```

---

## 4. 구현 상세

### 4.1 Entity

```java
@Entity
@Table(name = "diary")
public class Diary {
    @Id
    private String id;           // UUID 문자열

    @Column(nullable = false)
    private Long date;           // timestamp(ms)

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(name = "emotion_id", nullable = false)
    private Integer emotionId;   // 1~5

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist / @PreUpdate    // 자동 타임스탬프
}
```

### 4.2 Repository

```java
public interface DiaryRepository extends JpaRepository<Diary, String> {

    // 월별 조회: from <= date <= to
    List<Diary> findByDateBetweenOrderByDateDesc(Long from, Long to);  // latest
    List<Diary> findByDateBetweenOrderByDateAsc(Long from, Long to);   // oldest
}
```

### 4.3 DTO (요청/응답)

**DiaryRequest** (POST/PUT 공용)
```json
{
  "date": 1706054400000,
  "content": "일기 내용",
  "emotionId": 3
}
```
- `@NotNull`, `@Size(min=1, max=2000)`, `@Min(1) @Max(5)` 검증

**DiaryResponse** (단건)
```json
{
  "id": "uuid-1",
  "date": 1706054400000,
  "content": "오늘은 기분이 좋았다.",
  "emotionId": 2
}
```

**DiaryListResponse** (목록)
```json
{
  "items": [ DiaryResponse... ],
  "total": 1
}
```

**ErrorResponse** (에러)
```json
{
  "code": "VALIDATION_ERROR",
  "message": "content is required"
}
```

### 4.4 Controller 엔드포인트

```java
@RestController
@RequestMapping("/api/diaries")
public class DiaryController {

    @GetMapping                              // 월별 목록
    ResponseEntity<DiaryListResponse> list(
        @RequestParam Long from,
        @RequestParam Long to,
        @RequestParam(defaultValue = "latest") String sort
    )

    @GetMapping("/{id}")                     // 단건 조회
    ResponseEntity<DiaryResponse> getById(@PathVariable String id)

    @PostMapping                             // 생성
    ResponseEntity<DiaryResponse> create(@Valid @RequestBody DiaryRequest request)
    // → 201 Created

    @PutMapping("/{id}")                     // 수정
    ResponseEntity<DiaryResponse> update(
        @PathVariable String id,
        @Valid @RequestBody DiaryRequest request
    )

    @DeleteMapping("/{id}")                  // 삭제
    ResponseEntity<Void> delete(@PathVariable String id)
    // → 204 No Content
}
```

### 4.5 에러 처리 (GlobalExceptionHandler)

| 예외 | HTTP 상태 | code |
|------|-----------|------|
| `DiaryNotFoundException` | 404 | `NOT_FOUND` |
| `MethodArgumentNotValidException` | 400 | `VALIDATION_ERROR` |
| `기타 Exception` | 500 | `INTERNAL_ERROR` |

### 4.6 CORS 설정

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:5173")  // Vite 기본 포트
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowedHeaders("*");
    }
}
```

---

## 5. application.yml 설정

```yaml
server:
  port: 8989

spring:
  datasource:
    url: jdbc:mysql://192.168.219.199:9002/emotion_diary?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=utf8mb4
    username: root
    password: tjalsrn99(
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: validate    # 운영: validate, 개발 초기: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQLDialect
```

---

## 6. 작업 순서

### Phase 1: 프로젝트 셋업 (개발자가 직접 수행)
- [ ] Spring Initializr로 프로젝트 생성 (Spring Web, JPA, MySQL Driver, Validation)
- [ ] MySQL에 `emotion_diary` 데이터베이스 생성
- [ ] `application.yml` DB 접속 정보 설정
- [ ] 프로젝트 빌드 및 기동 확인

### Phase 2: DB 스키마 생성 (개발자가 직접 수행)
- [ ] `diary` 테이블 DDL 실행 (또는 JPA ddl-auto=update로 자동 생성)

### Phase 3: Entity + Repository 구현
- [ ] `Diary` 엔티티 클래스
- [ ] `DiaryRepository` 인터페이스 (월별 조회 쿼리 메서드)

### Phase 4: DTO 구현
- [ ] `DiaryRequest` (Bean Validation 포함)
- [ ] `DiaryResponse`
- [ ] `DiaryListResponse`
- [ ] `ErrorResponse`

### Phase 5: Service 구현
- [ ] `DiaryService` (CRUD + 정렬 로직)

### Phase 6: Controller 구현
- [ ] 5개 엔드포인트 구현
- [ ] 응답 상태 코드 정확히 매핑 (201, 204 등)

### Phase 7: 예외 처리
- [ ] `DiaryNotFoundException`
- [ ] `GlobalExceptionHandler` (@ControllerAdvice)

### Phase 8: CORS 설정
- [ ] `CorsConfig` 전역 CORS 허용

### Phase 9: 통합 테스트
- [ ] 각 엔드포인트 수동 테스트 (Postman 또는 curl)
- [ ] 프론트엔드 연동 테스트 (`VITE_DATA_SOURCE=api`)

---

## 7. 프론트 연동 시 체크리스트

- [ ] 프론트 `.env`에 `VITE_API_BASE_URL=http://localhost:8080` 설정
- [ ] 프론트 `.env`에 `VITE_DATA_SOURCE=api` 설정
- [ ] CORS 에러 없이 호출되는지 확인
- [ ] 날짜 timestamp(ms) 송수신 정합성 확인
- [ ] 에러 응답 형식(`code`, `message`) 프론트 핸들링 확인
- [ ] E2E 시나리오 통과: 작성 → 조회 → 수정 → 삭제

---

## 8. build.gradle 의존성 (참고)

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    runtimeOnly 'com.mysql:mysql-connector-j'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```
