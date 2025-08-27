# AI 서비스 전체 흐름 가이드

## 개요
AI 서비스는 사용자의 자연어 쿼리를 분석하여 커스텀 API를 생성하고, 실행하며, 게시판에 공유할 수 있는 기능을 제공합니다.

## 주요 기능
- 자연어 쿼리 분석 및 커스텀 API 생성
- 커스텀 API 실행 (AI+ 개인화 기능 포함)
- API 게시판 공유/제거 기능

---

## 1. 전체 시스템 흐름

### 1.1 API 생성 단계
```
사용자 → AI서비스 → 커스텀API서비스 → API관리서비스
```

### 1.2 API 실행 단계
```
사용자 → AI서비스 → 커스텀API서비스 → API관리서비스 → 외부API들 → AI개인화서비스(선택)
```

### 1.3 게시판 공유 단계 (기존 공유 API 사용)
```
사용자(대시보드) → 공유API서비스 (/api/shared-apis/{userId}/share)
프론트엔드(게시판) → 공유API서비스 (/api/shared-apis?page=0&size=20)
```

---

## 2. 상세 흐름

### 2.1 API 생성 흐름 (`POST /ai/analyze-query`)

#### 시점: 사용자가 새로운 API 생성을 원할 때

**입력:**
```json
{
  "query": "서울의 맛집 정보를 가져오는 API를 만들어줘",
  "custom_api_id": "custom-api-12345"
}
```

**처리 과정:**
1. **AI서비스**: Gemini LLM을 통해 자연어 쿼리 분석
    - 도메인 분류 (예: "RESTAURANT", "LOCATION")
    - 키워드 추출 (예: "search", "location")

2. **커스텀API서비스로 전달**: 분석 결과를 기반으로 API 생성 요청
   ```json
   {
     "userId": "auth0|user123",
     "customApiId": "custom-api-12345",
     "originalQuery": "서울의 맛집 정보를 가져오는 API를 만들어줘",
     "domains": ["RESTAURANT", "LOCATION"],
     "keywords": ["search", "location"]
   }
   ```

3. **커스텀API서비스**:
    - API 실행 계획(레시피) 생성
    - **사용자 개인 대시보드에만 등록** (게시판에는 등록되지 않음)

**출력:**
```json
{
  "status": "ACCEPTED",
  "message": "API 생성 요청이 성공적으로 분석되어 전달되었습니다.",
  "requestedApiId": "custom-api-12345",
  "analysisResult": {
    "detectedDomains": ["RESTAURANT", "LOCATION"],
    "detectedKeywords": ["search", "location"]
  }
}
```

**중요:** 이 시점에서는 아직 실제 사용 가능한 URL인지 확인하지 않습니다.

---

### 2.2 API 실행 흐름 (`GET /ai/execute/{customApiId}`)

#### 시점: 사용자가 생성된 API를 실제로 사용하고 싶을 때

**URL 예시:**
```
GET /ai/execute/custom-api-12345?query=location=서울&ai-plus=true
```

**처리 과정:**

#### Step 1: 레시피 조회
- **커스텀API서비스**에서 API 실행 계획(레시피) 조회
- 어떤 외부 API들을 어떤 순서로 호출할지 확인

#### Step 2: URL 유효성 확인 ⭐
- **API관리서비스**에 API ID 리스트 전달
- **실제 호출 가능한 URL로 변환** 받음
- 이 시점에서 실제 사용 가능한 API인지 확인됨

```java
// AIOrchestrationServiceImpl.java:48-54
ApiUrlResponse apiUrlResponse = apiManagementClient.getApiUrls(new ApiUrlRequest(apiIdsToResolve));
Map<String, String> apiUrlMap = apiUrlResponse.getApis().stream()
    .collect(Collectors.toMap(
        ApiUrlResponse.ApiUrlDetail::getApiId,
        ApiUrlResponse.ApiUrlDetail::getApiUrl
    ));
```

#### Step 3: 외부 API 호출
- 변환된 실제 URL들로 외부 API 순차 호출
- 결과를 컨텍스트에 누적

#### Step 4: AI+ 개인화 처리 ⭐
**AI+ 유무 체크 시점**: 모든 외부 API 호출이 완료된 후

```java
// AIOrchestrationServiceImpl.java:97-106
if (aiPlusEnabled && !userId.equals("anonymous")) {
    try {
        Map<String, Object> personalizedResult = aiPersonalizationService.personalize(userId, executionContext);
        return personalizedResult;
    } catch (Exception e) {
        log.warn("AI+ 서머리 생성 실패. 원본 데이터를 반환합니다. 오류: {}", e.getMessage());
    }
}
```

**AI+ 처리 상세:**
1. **무료 사용자**: BYOK(Bring Your Own Key) 확인
    - 키가 없으면: `RuntimeException("AI+ 기능을 사용하려면 먼저 API 키를 설정해주세요.")`
    - 키가 있으면: 사용자 키로 간단한 요약 생성

2. **PRO 사용자**: 시스템 키로 고급 분석(인사이트, 예측) 생성

**출력:**
- **AI+ 꺼짐**: `{"data": {원본데이터}}`
- **AI+ 켜짐**: `{"summary": "요약", "insights": [...], "predictions": "예측", "data": {원본데이터}}`

---

### 2.3 게시판 공유 흐름 (기존 공유 API 사용)

#### 2.3.1 API 공유 게시 (`POST /api/shared-apis/{userId}/share`)

**시점**: 사용자가 개인 대시보드에서 "공유하기" 버튼을 클릭할 때

**처리 과정:**
- 프론트엔드에서 직접 공유 API 서비스 호출
- AI 서비스를 거치지 않고 바로 공유 처리

#### 2.3.2 API 공유 게시 취소 (`DELETE /api/shared-apis/{userId}/unshare/{sharedApiId}`)

**시점**: 사용자가 공유를 취소하고 싶을 때

#### 2.3.3 기존 공유 API 명세 활용

**전체 공유 API 목록 조회**
```
GET /api/shared-apis?page=0&size=20
```

**내가 공유한 API 목록**
```
GET /api/shared-apis/{userId}/my
```

**공유 API 저장 (북마크)**
```
POST /api/shared-apis/{userId}/save
```

**저장된 API 목록**
```
GET /api/shared-apis/{userId}/saved
```

**저장된 API 삭제**
```
DELETE /api/shared-apis/{userId}/saved/{userApiId}
```

**공유 API 검색**
```
GET /api/shared-apis/search?keyword={keyword}
```

**공유된 URL 형태**: `/ai/execute/custom-api-12345?query=location=서울`
- 다른 사용자들이 이 URL을 사용할 때 각자 `&ai-plus=true/false` 추가 가능

---

## 3. 주요 확인 시점 정리

| 확인 항목 | 시점 | 담당 서비스 | 상세 |
|----------|------|-----------|------|
| **자연어 분석** | API 생성 요청 시 | AI서비스 | Gemini LLM을 통한 도메인/키워드 분류 |
| **실제 URL 유효성** | API 실행 시 | API관리서비스 | API ID → 실제 호출 가능한 URL 변환 |
| **AI+ 권한 확인** | API 실행 후 | AI개인화서비스 | BYOK 키 존재 여부 확인 |
| **공유 권한 확인** | 게시판 공유 시 | 공유API서비스 | 자신의 API인지 확인 |

---

## 4. 사용자 시나리오

### 시나리오 1: 일반적인 API 생성 및 사용
1. **생성**: `POST /ai/analyze-query` → 개인 대시보드에 등록
2. **실행**: `GET /ai/execute/custom-api-12345?query=location=서울&ai-plus=false`
3. **공유**: `POST /api/shared-apis/{userId}/share` → 게시판에 공개

### 시나리오 2: AI+ 기능 사용 (무료 사용자)
1. **키 설정**: AWS Secrets Manager에 개인 Gemini API 키 등록
2. **실행**: `GET /ai/execute/custom-api-12345?query=location=서울&ai-plus=true`
3. **결과**: 개인 키로 생성된 요약 포함

### 시나리오 3: AI+ 기능 사용 실패 (키 없음)
1. **실행**: `GET /ai/execute/custom-api-12345?query=location=서울&ai-plus=true`
2. **오류**: `"AI+ 기능을 사용하려면 먼저 API 키를 설정해주세요."`
3. **결과**: API 실행 중단

### 시나리오 4: 게시판에서 다른 사용자 API 사용
1. **발견**: 게시판에서 `/ai/execute/other-user-api-999?query=location=부산` 발견
2. **사용**: `GET /ai/execute/other-user-api-999?query=location=부산&ai-plus=true`
3. **결과**: 자신의 AI+ 설정으로 개인화된 결과 받음

---

## 5. 에러 처리

### API 실행 시 발생 가능한 오류
- **404**: 커스텀 API를 찾을 수 없음
- **500**: 외부 API 호출 실패
- **RuntimeException**: AI+ 키 설정 오류

### 게시판 공유 시 발생 가능한 오류
- **403**: 권한 없음 (다른 사용자의 API)
- **404**: 해당 API를 찾을 수 없음
- **500**: 커스텀API 서비스 통신 실패

---

## 6. 향후 개선 사항

1. **게시판 서비스 클라이언트**: 현재 커스텀API 서비스를 통해 우회, 향후 직접 연동
2. **실제 AWS Secrets Manager 연동**: 현재 시스템 키로 임시 처리
3. **API 상태 조회**: 생성 진행 상황을 실시간으로 확인할 수 있는 API
4. **배치 공유**: 여러 API를 한 번에 공유/제거하는 기능

---

## 7. 프론트엔드에서 사용하는 방법

### 7.1 개인 대시보드 페이지
**사용자별 API 목록 조회:**
```javascript
// 현재 로그인한 사용자의 API 목록
GET /custom-apis?userId={currentUserId}

// 응답 예시
{
  "apis": [
    {
      "customApiId": "custom-api-12345",
      "title": "서울 맛집 검색 API",
      "description": "서울의 맛집 정보를 가져오는 API",
      "isShared": false,
      "createdAt": "2024-01-01T10:00:00Z"
    }
  ]
}
```

**공유하기 버튼 클릭 시:**
```javascript
// AI 서비스의 공유 API 호출
POST /ai/board/share/custom-api-12345
Headers: Authorization: Bearer {jwt_token}

// 성공 응답
{
  "success": true,
  "message": "API가 게시판에 성공적으로 공유되었습니다."
}
```

### 7.2 API 게시판 페이지
**공유된 API 목록 조회:**
```javascript
// 모든 사용자가 공유한 API 목록
GET /custom-apis?isShared=true

// 응답 예시
{
  "sharedApis": [
    {
      "customApiId": "other-user-api-999",
      "title": "부산 관광지 추천 API",
      "description": "부산의 관광지를 추천하는 API",
      "author": "user@example.com",
      "usageUrl": "/ai/execute/other-user-api-999?query=location=부산",
      "createdAt": "2024-01-01T10:00:00Z",
      "tags": ["TOURISM", "LOCATION"]
    }
  ]
}
```

**게시판에서 API 사용:**
```javascript
// 게시판에서 본 API를 실제 사용
GET /ai/execute/other-user-api-999?query=location=부산&ai-plus=true
Headers: Authorization: Bearer {my_jwt_token}

// 내 AI+ 설정으로 개인화된 결과 받음
{
  "summary": "부산의 인기 관광지 요약...",
  "data": { /* 원본 관광지 데이터 */ }
}
```

### 7.3 공유 해제
**공유 해제 버튼 클릭 시:**
```javascript
// AI 서비스의 공유 해제 API 호출
DELETE /ai/board/unshare/custom-api-12345
Headers: Authorization: Bearer {jwt_token}

// 성공 응답
{
  "success": true,
  "message": "API가 게시판에서 성공적으로 제거되었습니다."
}
```

### 7.4 UI/UX 권장 사항

#### 개인 대시보드
- **공유 상태 표시**: 각 API 카드에 "공유됨" 배지 표시
- **공유 토글**: 간단한 스위치로 공유/비공유 전환
- **사용 통계**: 공유된 API의 사용 횟수 표시

#### 게시판
- **검색/필터**: 도메인, 키워드별 필터링
- **인기도 정렬**: 사용 횟수, 평점별 정렬
- **미리보기**: API 설명과 예상 결과 미리보기
- **AI+ 표시**: 각 사용자가 AI+ 사용 가능 여부 표시

#### 공통
- **로딩 상태**: API 호출 중 로딩 스피너
- **에러 처리**: 명확한 에러 메시지 표시
- **권한 확인**: 자신의 API만 공유/해제 가능하도록 UI 제한

---

## 8. 주요 파일 위치

```
src/main/java/org/example/AIsvc/
├── controller/
│   └── AIController.java                    # API 엔드포인트
├── service/
│   ├── AIServiceImpl.java                  # 메인 비즈니스 로직
│   ├── AIOrchestrationServiceImpl.java     # API 실행 오케스트레이션
│   └── AIPersonalizationServiceImpl.java   # AI+ 개인화 기능
├── client/
│   ├── CustomApiClient.java                # 커스텀API 서비스 연동
│   ├── ApiManagementClient.java            # API관리 서비스 연동
│   └── GeminiClient.java                   # Gemini LLM 연동
└── dto/
    ├── request/AnalyzeQueryRequest.java     # API 생성 요청
    └── custom_api/InitiateCreationRequest.java  # 커스텀API 생성 요청
```
