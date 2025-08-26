# 커스텀 API 생성 완료 후 AI 서비스 응답 형태 및 사용법

## 개요
커스텀 API 생성 요청 완료 후 "AI 서비스"가 받게 되는 응답 데이터 형태와 실제 활용 방법을 안내합니다.

---

## 1. AI 서비스가 받는 응답 JSON 구조

### 성공 응답 예시
```json
{
  "success": true,
  "errorCode": null,
  "message": "커스텀 API가 성공적으로 생성되었습니다.",
  "data": {
    "customApiId": "custom-api-12345",
    "userId": "user-auth0-abc123",
    "name": "사용자 위치 기반 날씨 및 맛집 추천 API",
    "description": "사용자의 현재 위치를 기반으로 실시간 날씨 정보와 주변 맛집 정보를 통합하여 제공하는 커스텀 API입니다. 날씨에 따른 적절한 음식 카테고리도 함께 추천합니다.",
    "externalApiUrl_list": [
      {
        "apiId": "weather-api-001",
        "apiName": "OpenWeatherMap 현재 날씨 API",
        "parameters": [
          {
            "paramName": "latitude",
            "paramType": "INPUT",
            "description": "위도 좌표",
            "necessary": true
          },
          {
            "paramName": "longitude", 
            "paramType": "INPUT",
            "description": "경도 좌표",
            "necessary": true
          },
          {
            "paramName": "temperature",
            "paramType": "OUTPUT",
            "description": "현재 기온 (°C)",
            "necessary": false
          },
          {
            "paramName": "weather_condition",
            "paramType": "OUTPUT", 
            "description": "날씨 상태 (맑음, 흐림, 비 등)",
            "necessary": false
          }
        ]
      },
      {
        "apiId": "restaurant-api-002",
        "apiName": "Yelp 맛집 검색 API",
        "parameters": [
          {
            "paramName": "latitude",
            "paramType": "INPUT",
            "description": "위도 좌표",
            "necessary": true
          },
          {
            "paramName": "longitude",
            "paramType": "INPUT", 
            "description": "경도 좌표",
            "necessary": true
          },
          {
            "paramName": "radius",
            "paramType": "INPUT",
            "description": "검색 반경 (미터)",
            "necessary": false
          },
          {
            "paramName": "restaurant_list",
            "paramType": "OUTPUT",
            "description": "맛집 목록 정보",
            "necessary": false
          }
        ]
      },
      {
        "apiId": "recommendation-api-003",
        "apiName": "날씨별 음식 추천 API",
        "parameters": [
          {
            "paramName": "weather_condition",
            "paramType": "INPUT",
            "description": "날씨 상태",
            "necessary": true
          },
          {
            "paramName": "temperature",
            "paramType": "INPUT",
            "description": "기온",
            "necessary": true
          },
          {
            "paramName": "recommended_food_categories",
            "paramType": "OUTPUT",
            "description": "추천 음식 카테고리 목록",
            "necessary": false
          }
        ]
      }
    ],
    "createdAt": "2024-08-10T14:30:25.123456",
    "updatedAt": "2024-08-10T14:30:25.123456"
  },
  "timestamp": "2024-08-10T14:30:25.123456"
}
```

### 실패 응답 예시
```json
{
  "success": false,
  "errorCode": "EXTERNAL_API_UNAVAILABLE",
  "message": "외부 API 서비스에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.",
  "data": null,
  "timestamp": "2024-08-10T14:30:25.123456"
}
```

---

## 2. 응답 데이터 구조 상세 설명

### 기본 래퍼 구조 (BaseResponse)
| 필드명 | 타입 | 설명 |
|--------|------|------|
| `success` | boolean | API 요청 성공 여부 (true: 성공, false: 실패) |
| `errorCode` | string \| null | 오류 발생 시 오류 코드 |
| `message` | string | 응답 메시지 (성공/실패 상황 설명) |
| `data` | CustomApiResponseDto \| null | 실제 커스텀 API 데이터 |
| `timestamp` | string | 응답 생성 시각 (ISO 8601 형식) |

### 커스텀 API 데이터 구조 (CustomApiResponseDto)
| 필드명 | 타입 | 설명 |
|--------|------|------|
| `customApiId` | string | 생성된 커스텀 API의 고유 식별자 |
| `userId` | string | 요청한 사용자의 ID |
| `name` | string | AI가 생성한 커스텀 API의 이름 |
| `description` | string | 커스텀 API에 대한 상세 설명 |
| `externalApiUrl_list` | ExternalApiInfoDto[] | 선별된 외부 API들의 목록 |
| `createdAt` | string | 생성 일시 |
| `updatedAt` | string | 수정 일시 |

### 외부 API 정보 구조 (ExternalApiInfoDto)
| 필드명 | 타입 | 설명 |
|--------|------|------|
| `apiId` | string | 외부 API의 고유 식별자 |
| `apiName` | string | 외부 API의 이름 |
| `parameters` | ApiParameter[] | API의 입력/출력 파라미터 목록 |

### API 파라미터 구조 (ApiParameter)
| 필드명 | 타입 | 설명 |
|--------|------|------|
| `paramName` | string | 파라미터 이름 |
| `paramType` | string | 파라미터 타입 ("INPUT" 또는 "OUTPUT") |
| `description` | string | 파라미터에 대한 설명 |
| `necessary` | boolean | 필수 여부 (INPUT 파라미터만 해당) |

---

## 3. AI 서비스에서의 활용 방법

### 3.1 응답 검증
```javascript
// 응답 검증 예시 코드
function validateCustomApiResponse(response) {
    // 기본 응답 구조 검증
    if (!response || typeof response.success !== 'boolean') {
        throw new Error('Invalid response format');
    }
    
    // 성공 응답인 경우 데이터 검증
    if (response.success && response.data) {
        const customApi = response.data;
        
        // 필수 필드 검증
        if (!customApi.customApiId || !customApi.name || !customApi.externalApiUrl_list) {
            throw new Error('Missing required fields in custom API data');
        }
        
        // 외부 API 목록 검증
        if (!Array.isArray(customApi.externalApiUrl_list) || customApi.externalApiUrl_list.length === 0) {
            throw new Error('Invalid external API list');
        }
        
        return true;
    }
    
    // 실패 응답인 경우 에러 정보 검증
    if (!response.success) {
        console.error(`Custom API creation failed: ${response.errorCode} - ${response.message}`);
        return false;
    }
}
```

### 3.2 API 호출 순서 분석
```javascript
// 데이터 의존성 기반 API 호출 순서 분석
function analyzeApiCallOrder(externalApiList) {
    const callOrder = [];
    const parallelGroups = [];
    
    externalApiList.forEach(api => {
        const inputParams = api.parameters.filter(p => p.paramType === 'INPUT');
        const outputParams = api.parameters.filter(p => p.paramType === 'OUTPUT');
        
        // 데이터 의존성 분석 로직
        // (AI가 이미 분석한 호출 순서 정보를 활용)
    });
    
    return { callOrder, parallelGroups };
}
```

### 3.3 실제 API 오케스트레이션
```javascript
// 커스텀 API 실행 예시
async function executeCustomApi(customApiData, userInput) {
    const { externalApiUrl_list } = customApiData;
    
    // 1. 사용자 입력 데이터 매핑
    const executionContext = mapUserInputToApiParams(userInput, externalApiUrl_list);
    
    // 2. API 호출 순서대로 실행
    const results = {};
    
    for (const api of externalApiUrl_list) {
        try {
            // 입력 파라미터 준비
            const inputParams = prepareInputParams(api, executionContext, results);
            
            // 외부 API 호출
            const apiResult = await callExternalApi(api.apiId, inputParams);
            
            // 결과 저장
            results[api.apiId] = apiResult;
            
            // 출력 파라미터를 다음 API의 입력으로 사용할 수 있도록 컨텍스트 업데이트
            updateExecutionContext(executionContext, api, apiResult);
            
        } catch (error) {
            console.error(`Failed to call API ${api.apiName}:`, error);
            throw error;
        }
    }
    
    // 3. 최종 결과 조합 및 반환
    return combineResults(results, externalApiList);
}
```

---

## 4. 오류 처리 가이드

### 주요 오류 코드
| 오류 코드 | 설명 | 처리 방법 |
|-----------|------|----------|
| `INVALID_REQUEST` | 요청 데이터가 유효하지 않음 | 요청 형식 및 필수 필드 확인 |
| `EXTERNAL_API_UNAVAILABLE` | 외부 API 서비스 접근 불가 | 재시도 또는 대체 서비스 활용 |
| `GEMINI_API_ERROR` | AI 모델 처리 중 오류 | 프롬프트 수정 후 재시도 |
| `PLAN_LIMIT_EXCEEDED` | 플랜별 API 개수 제한 초과 | 요청 범위 축소 또는 플랜 업그레이드 안내 |

### 오류 처리 예시
```javascript
function handleCustomApiError(response) {
    switch (response.errorCode) {
        case 'EXTERNAL_API_UNAVAILABLE':
            // 재시도 로직
            setTimeout(() => retryCustomApiCreation(), 5000);
            break;
            
        case 'PLAN_LIMIT_EXCEEDED':
            // 사용자에게 플랜 업그레이드 안내
            showUpgradePlanNotification();
            break;
            
        case 'GEMINI_API_ERROR':
            // 다른 요청 방식으로 재시도
            reformatRequestAndRetry();
            break;
            
        default:
            // 일반적인 오류 처리
            showErrorMessage(response.message);
    }
}
```

---

## 5. 성능 최적화 팁

### 5.1 병렬 처리 활용
- `externalApiUrl_list`에서 데이터 의존성이 없는 API들을 식별하여 병렬 호출
- AI가 제공한 호출 순서 정보를 활용하여 최적의 실행 계획 수립

### 5.2 결과 캐싱
```javascript
// API 결과 캐싱 예시
const apiResultCache = new Map();

function getCachedResult(apiId, params) {
    const cacheKey = `${apiId}_${JSON.stringify(params)}`;
    return apiResultCache.get(cacheKey);
}

function setCachedResult(apiId, params, result) {
    const cacheKey = `${apiId}_${JSON.stringify(params)}`;
    apiResultCache.set(cacheKey, result);
}
```

### 5.3 타임아웃 설정
```javascript
const API_TIMEOUT = 10000; // 10초

async function callExternalApiWithTimeout(apiId, params) {
    return Promise.race([
        callExternalApi(apiId, params),
        new Promise((_, reject) => 
            setTimeout(() => reject(new Error('API call timeout')), API_TIMEOUT)
        )
    ]);
}
```

---

## 6. 사용 시나리오 예시

### 시나리오: 위치 기반 날씨-맛집 추천 서비스
1. **사용자 입력**: `{ "latitude": 37.5665, "longitude": 126.9780 }`
2. **API 실행 순서**:
   - **1단계**: 날씨 API 호출 (위도/경도 → 날씨 정보)
   - **2단계**: 맛집 검색 API 호출 (위도/경도 → 주변 맛집)
   - **3단계**: 날씨별 음식 추천 API 호출 (날씨 정보 → 추천 음식 카테고리)
3. **최종 결과**: 통합된 날씨 정보 + 맛집 목록 + 날씨에 적합한 음식 추천

이러한 구조를 통해 AI 서비스는 복잡한 비즈니스 로직을 간단한 API 호출로 처리할 수 있으며, 사용자에게 보다 가치 있는 통합 서비스를 제공할 수 있습니다.