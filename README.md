# Emotion Diary API Server

감정일기장 백엔드 REST API 서버입니다. 사용자가 매일의 감정과 일기를 기록하고 관리할 수 있는 CRUD API를 제공합니다.

## 기술 스택

| 구분 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 4.0.3 |
| ORM | Spring Data JPA (Hibernate) |
| Database | MySQL 8 |
| Build | Gradle 9.3.1 |
| Library | Lombok, Jakarta Validation |
| Container | Docker |

## 프로젝트 구조

```
src/main/java/it/codro/emotiondiary/
├── EmotiondiaryApplication.java        # 애플리케이션 진입점
├── config/
│   └── CorsConfig.java                 # CORS 설정
├── controller/
│   └── DiaryController.java            # REST API 컨트롤러
├── service/
│   └── DiaryService.java               # 비즈니스 로직
├── repository/
│   └── DiaryRepository.java            # JPA 레포지토리
├── entity/
│   └── Diary.java                      # JPA 엔티티
├── dto/
│   ├── DiaryRequest.java               # 요청 DTO
│   ├── DiaryResponse.java              # 응답 DTO
│   ├── DiaryListResponse.java          # 목록 응답 DTO
│   └── ErrorResponse.java              # 에러 응답 DTO
└── exception/
    ├── DiaryNotFoundException.java      # 404 커스텀 예외
    └── GlobalExceptionHandler.java     # 전역 예외 핸들러
```

## API 명세

Base URL: `/api/diaries`

| Method | Endpoint | 설명 | 상태 코드 |
|--------|----------|------|-----------|
| `GET` | `/api/diaries` | 일기 목록 조회 | 200 |
| `GET` | `/api/diaries/{id}` | 일기 단건 조회 | 200 / 404 |
| `POST` | `/api/diaries` | 일기 작성 | 201 / 400 |
| `PUT` | `/api/diaries/{id}` | 일기 수정 | 200 / 400 / 404 |
| `DELETE` | `/api/diaries/{id}` | 일기 삭제 | 204 / 404 |

### 목록 조회 쿼리 파라미터

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `from` | Long | 시작 날짜 (Unix timestamp ms) |
| `to` | Long | 종료 날짜 (Unix timestamp ms) |
| `sort` | String | 정렬 기준 (`latest` / `oldest`) |

### 요청 Body (POST / PUT)

```json
{
  "date": 1699564800000,
  "content": "오늘 하루 기분이 좋았다.",
  "emotionId": 1
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `date` | Long | O | 날짜 (Unix timestamp ms) |
| `content` | String | O | 일기 내용 (1~2000자) |
| `emotionId` | Integer | O | 감정 코드 (1~5) |

### 감정 코드

| ID | 감정 |
|----|------|
| 1 | 매우 좋음 |
| 2 | 좋음 |
| 3 | 보통 |
| 4 | 나쁨 |
| 5 | 매우 나쁨 |

### 응답 형식

**단건 응답 (DiaryResponse)**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "date": 1699564800000,
  "content": "오늘 하루 기분이 좋았다.",
  "emotionId": 1
}
```

**목록 응답 (DiaryListResponse)**
```json
{
  "items": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "date": 1699564800000,
      "content": "오늘 하루 기분이 좋았다.",
      "emotionId": 1
    }
  ],
  "total": 1
}
```

**에러 응답 (ErrorResponse)**
```json
{
  "code": "NOT_FOUND",
  "message": "Diary not found with id: ..."
}
```

| code | HTTP 상태 | 설명 |
|------|-----------|------|
| `NOT_FOUND` | 404 | 해당 ID의 일기를 찾을 수 없음 |
| `VALIDATION_ERROR` | 400 | 요청 데이터 유효성 검증 실패 |
| `INTERNAL_ERROR` | 500 | 서버 내부 오류 |

## 데이터베이스 스키마

```sql
CREATE DATABASE IF NOT EXISTS emotion_diary
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE diary (
    id          VARCHAR(36)   NOT NULL PRIMARY KEY,
    date        BIGINT        NOT NULL COMMENT 'Unix timestamp(ms)',
    content     VARCHAR(2000) NOT NULL,
    emotion_id  INT           NOT NULL,
    created_at  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                              ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_emotion_id CHECK (emotion_id BETWEEN 1 AND 5),
    INDEX idx_date (date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

## 실행 방법

### 로컬 실행

```bash
# 빌드
./gradlew build

# 실행
./gradlew bootRun
```

서버가 `http://localhost:8989`에서 실행됩니다.

### 환경 변수

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://192.168.219.199:9002/emotion_diary...` | DB 접속 URL |
| `SPRING_DATASOURCE_USERNAME` | `root` | DB 사용자명 |
| `SPRING_DATASOURCE_PASSWORD` | - | DB 비밀번호 |

### Docker 실행

```bash
# 빌드
docker build -t emotiondiary .

# 실행
docker run -d -p 8989:8989 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host:3306/emotion_diary \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=yourpassword \
  emotiondiary
```

## 설계 특징

- **UUID PK** : `@PrePersist`에서 애플리케이션 레벨로 UUID를 생성하여 응답 시 즉시 반환
- **JPA Dirty Checking** : 수정 시 명시적 `save()` 호출 없이 트랜잭션 내 변경 감지로 자동 반영
- **클래스 레벨 `readOnly` 트랜잭션** : 읽기 전용 기본 설정 후 쓰기 메서드만 개별 `@Transactional` 적용
- **전역 예외 처리** : `@RestControllerAdvice`로 일관된 에러 응답 형식 제공
- **인증 없음** : 개인용 감정일기 특성상 인증 레이어 미적용
- **CORS** : 프론트엔드 개발 서버(`localhost:5173`) 허용