# 감정 일기장 API 명세서

## Base URL

```
https://emotiondiary-api.codro.it
```

## CORS

| 항목 | 값 |
|------|-----|
| Allowed Origins | `http://localhost:5173` |
| Allowed Methods | GET, POST, PUT, DELETE |
| Allowed Headers | `*` |

---

## 엔드포인트

### 1. 일기 목록 조회

```
GET /api/diaries?from={from}&to={to}&sort={sort}
```

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| from | Long | O | - | 시작 날짜 (timestamp ms) |
| to | Long | O | - | 종료 날짜 (timestamp ms) |
| sort | String | X | `latest` | 정렬 (`latest` = 최신순, `oldest` = 오래된순) |

**Response** `200 OK`

```json
{
  "items": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "date": 1699564800000,
      "content": "오늘 하루 정말 좋았다",
      "emotionId": 5
    }
  ],
  "total": 1
}
```

---

### 2. 일기 단건 조회

```
GET /api/diaries/{id}
```

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| id | String (UUID) | 일기 ID |

**Response** `200 OK`

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "date": 1699564800000,
  "content": "오늘 하루 정말 좋았다",
  "emotionId": 5
}
```

---

### 3. 일기 생성

```
POST /api/diaries
Content-Type: application/json
```

**Request Body**

```json
{
  "date": 1699564800000,
  "content": "오늘 하루 정말 좋았다",
  "emotionId": 5
}
```

| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|------|------|------|----------|------|
| date | Long | O | - | 날짜 (timestamp ms) |
| content | String | O | 1~2000자 | 일기 내용 |
| emotionId | Integer | O | 1~5 | 감정 ID |

**Response** `201 Created`

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "date": 1699564800000,
  "content": "오늘 하루 정말 좋았다",
  "emotionId": 5
}
```

---

### 4. 일기 수정

```
PUT /api/diaries/{id}
Content-Type: application/json
```

**Request Body** — 생성과 동일

**Response** `200 OK` — 수정된 일기 객체 반환

---

### 5. 일기 삭제

```
DELETE /api/diaries/{id}
```

**Response** `204 No Content` — 본문 없음

---

## 에러 응답

모든 에러는 동일한 형식으로 반환됩니다.

```json
{
  "code": "ERROR_CODE",
  "message": "에러 메시지"
}
```

| HTTP Status | code | 발생 조건 |
|-------------|------|-----------|
| 400 | `VALIDATION_ERROR` | 필수값 누락, content 길이 초과, emotionId 범위 벗어남 |
| 404 | `NOT_FOUND` | 존재하지 않는 일기 ID로 조회/수정/삭제 |
| 500 | `INTERNAL_ERROR` | 서버 내부 오류 |

**Validation 에러 메시지**

| 필드 | 메시지 |
|------|--------|
| date | `date is required` |
| content | `content is required` |
| content | `content must be between 1 and 2000 characters` |
| emotionId | `emotionId is required` |
| emotionId | `emotionId must be between 1 and 5` |

---

## 응답 타입 정리 (TypeScript)

```typescript
interface DiaryResponse {
  id: string;
  date: number;
  content: string;
  emotionId: number; // 1~5
}

interface DiaryListResponse {
  items: DiaryResponse[];
  total: number;
}

interface DiaryRequest {
  date: number;
  content: string;   // 1~2000자
  emotionId: number;  // 1~5
}

interface ErrorResponse {
  code: string;
  message: string;
}
```
