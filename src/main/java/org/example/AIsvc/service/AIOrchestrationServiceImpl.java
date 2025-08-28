package org.example.AIsvc.service;

// ## import: 필요한 클래스들을 가져옵니다.
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows; // ## Lombok: 체크 예외(checked exception)를 언체크 예외로 변환하여 코드를 간결하게 만듭니다.
import lombok.extern.slf4j.Slf4j;
import org.example.AIsvc.client.CustomApiClient;
import org.example.AIsvc.client.GenericApiClient;
import org.example.AIsvc.client.GeminiClient;
import org.example.AIsvc.dto.gemini.GeminiRequest;
import org.example.AIsvc.dto.gemini.GeminiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.example.AIsvc.dto.execution.ApiParameterDto;
import org.example.AIsvc.dto.execution.CustomApiResponseDto;
import org.example.AIsvc.dto.execution.ExternalApiInfoDto;
import org.example.AIsvc.dto.common.BaseResponse;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIOrchestrationServiceImpl implements AIOrchestrationService {

    private final CustomApiClient customApiClient;
    private final GenericApiClient genericApiClient;
    private final GeminiClient geminiClient;
    private final AIPersonalizationService aiPersonalizationService;
    
    @Value("${gemini.api.key}")
    private String geminiApiKey;
    
    @Value("${gemini.model}")
    private String model;

    @SneakyThrows
    @Override
    public Object executeCustomApi(String customApiId, String query, String userId, String aiPlusActive) {
        boolean isAiPlusEnabled = aiPlusActive != null && !aiPlusActive.trim().isEmpty();
        log.info("커스텀 API 실행 시작. API ID: {}, 쿼리: {}, 사용자: {}, AI+: {}, 분석요구사항: {}", 
                customApiId, query, userId, isAiPlusEnabled, aiPlusActive);

        //  1. `커스텀API 서비스`를 호출하여 API 실행 계획('레시피')을 가져옴
        BaseResponse<CustomApiResponseDto> response = customApiClient.getApiDetails(customApiId);
        
        // 디버깅: BaseResponse 전체 확인
        try {
            com.fasterxml.jackson.databind.ObjectMapper debugMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            debugMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            String responseJson = debugMapper.writeValueAsString(response);
            log.info("받아온 BaseResponse (JSON): {}", responseJson);
        } catch (Exception e) {
            log.warn("BaseResponse JSON 변환 실패: {}", e.getMessage());
        }
        
        // BaseResponse에서 성공 여부 확인 후 data 추출
        if (response == null || !response.isSuccess()) {
            String errorMessage = response != null ? response.getMessage() : "Unknown error occurred";
            log.error("커스텀 API 정보 조회 실패: {}", errorMessage);
            throw new IllegalStateException("커스텀 API 정보를 가져올 수 없습니다. API ID: " + customApiId);
        }
        
        CustomApiResponseDto recipe = response.getData();
        
        // 디버깅: 받아온 레시피 데이터 확인 (JSON 형태로 출력)
        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            String recipeJson = objectMapper.writeValueAsString(recipe);
            log.info("받아온 CustomApiResponseDto (JSON): {}", recipeJson);
        } catch (Exception e) {
            log.warn("JSON 변환 실패, 기본 toString() 사용: {}", recipe);
        }
        
        if (recipe != null) {
            log.info("Recipe 기본 정보 - ID: {}, 이름: {}", recipe.getCustomApiId(), recipe.getName());
            if (recipe.getExternalApiInfoList() != null) {
                log.info("ExternalApiInfoList 크기: {}", recipe.getExternalApiInfoList().size());
            } else {
                log.error("ExternalApiInfoList가 null입니다!");
            }
        } else {
            log.error("CustomApiResponseDto가 null입니다!");
            throw new IllegalStateException("커스텀 API 정보를 가져올 수 없습니다. API ID: " + customApiId);
        }

        // 2. 실행할 외부 API가 있는지 확인
        if (recipe.getExternalApiInfoList() == null || recipe.getExternalApiInfoList().isEmpty()) {
            log.error("실행할 외부 API가 없습니다. customApiId: {}", customApiId);
            throw new IllegalStateException("실행할 외부 API가 없습니다.");
        }
        
        log.info("{}개의 외부 API 실행을 시작합니다.", recipe.getExternalApiInfoList().size());

        // 3. 모든 API 호출 결과를 저장하고, 다음 API의 입력으로 사용될 데이터 저장소(컨텍스트)를 생성
        Map<String, Object> executionContext = new HashMap<>();

        // 최초 사용자 자연어 쿼리를 컨텍스트에 추가
        executionContext.put("INITIAL_QUERY", query);

        // 3. 적힌 순서대로 외부 API를 하나씩 호출
        for (ExternalApiInfoDto apiInfo : recipe.getExternalApiInfoList()) {
            log.info("단계 실행: {}", apiInfo.getApiName());

            // 3-1. AI에게 현재 API 정보와 컨텍스트를 전달하여 필요한 파라미터를 지능적으로 준비
            log.info("AI에게 파라미터 준비 요청: {}", apiInfo.getApiName());
            
            // AI에게 제공할 자연어 쿼리 분석 프롬프트 구성
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("당신은 정확하고 엄격한 API 파라미터 분석 전문가입니다.\n");
            promptBuilder.append("사용자의 자연어 요청을 분석해서 API 호출에 필요한 파라미터를 추출해주세요.\n\n");
            
            promptBuilder.append("호출할 API:\n");
            promptBuilder.append("- 이름: ").append(apiInfo.getApiName()).append("\n");
            promptBuilder.append("- 설명: 이 API는 다음 파라미터들을 필요로 합니다.\n");
            
            for (ApiParameterDto param : apiInfo.getParameters()) {
                // OUTPUT 파라미터가 아닌 모든 파라미터를 INPUT으로 처리
                if (!"OUTPUT".equals(param.getParamType())) {
                    promptBuilder.append("  * ").append(param.getParamName())
                               .append(" (").append(param.getParamType()).append(")")
                               .append(": ").append(param.getDescription())
                               .append(param.isNecessary() ? " (필수)" : " (선택사항)").append("\n");
                }
            }
            
            promptBuilder.append("\n사용자 요청: \"").append(query).append("\"\n");
            
            if (executionContext.size() > 1) { // INITIAL_QUERY 외에 다른 결과가 있는지 확인
                promptBuilder.append("\n이전 단계에서 얻은 데이터:\n");
                executionContext.entrySet().forEach(entry -> {
                    if (!"INITIAL_QUERY".equals(entry.getKey())) {
                        promptBuilder.append("- ").append(entry.getKey()).append(" 결과: ").append(entry.getValue()).append("\n");
                    }
                });
                promptBuilder.append("필요하다면 이전 데이터도 활용하세요.\n");
            }
            
            promptBuilder.append("\n**작업 지침:**\n");
            promptBuilder.append("1. 위에 명시된 각 파라미터를 개별적으로 분석하세요.\n");
            promptBuilder.append("2. 필수 파라미터는 반드시 포함해야 합니다.\n");
            promptBuilder.append("3. 각 파라미터는 하나의 값만 가져야 합니다 (쉼표로 구분된 여러 값 금지).\n");
            promptBuilder.append("4. 파라미터 이름은 정확히 위에 정의된 이름을 사용하세요.\n");
            promptBuilder.append("5. 사용자 요청에서 값을 추출할 수 없는 선택 파라미터는 제외하세요.\n\n");
            
            promptBuilder.append("**필수 파라미터 처리 (절대 규칙):**\n");
            promptBuilder.append("- 위에 '(필수)'로 표시된 모든 파라미터는 반드시 응답에 포함되어야 합니다.\n");
            promptBuilder.append("- 필수 파라미터가 하나라도 누락되면 API 호출이 실패합니다.\n");
            promptBuilder.append("- 필수 파라미터에 대한 값이 사용자 요청에서 명확하지 않으면 합리적인 기본값을 사용하세요.\n");
            promptBuilder.append("- 예시: 암호화폐 관련 요청이면 ids=\"bitcoin\", 통화 관련이면 vs_currencies=\"usd\" 등\n");
            promptBuilder.append("- 필수 파라미터를 빈 값(\"\")으로 두지 마세요.\n\n");
            
            promptBuilder.append("**데이터 타입 규칙:**\n");
            promptBuilder.append("- String 타입: 문자열 값 (예: \"bitcoin\", \"usd\")\n");
            promptBuilder.append("- Integer 타입: 숫자 값 (예: 1, 100, 365) - 따옴표 없음\n");
            promptBuilder.append("- Boolean 타입: true 또는 false - 따옴표 없음\n\n");
            
            promptBuilder.append("**절대 금지 사항:**\n");
            promptBuilder.append("- 위에 정의되지 않은 새로운 파라미터 이름을 만들지 마세요 (예: from, to, date_range 등)\n");
            promptBuilder.append("- 파라미터 이름을 변경하거나 수정하지 마세요\n");
            promptBuilder.append("- 하나의 파라미터에 여러 값을 쉼표로 구분해서 넣지 마세요\n");
            promptBuilder.append("- Integer 타입에 문자열이나 불린 값을 넣지 마세요\n\n");
            
            promptBuilder.append("사용자 요청을 분석해서 위 지침에 따라 순수한 JSON 형태로 응답해주세요.\n");
            promptBuilder.append("중요: 마크다운 코드 블록(```)을 사용하지 말고 순수한 JSON만 응답하세요.\n");
            promptBuilder.append("응답 형식: {\"파라미터명\": \"값\", \"파라미터명2\": \"값2\"}");
            
            // AI에게 보낸 전체 프롬프트 로그 출력
            String fullPrompt = promptBuilder.toString();
            log.info("AI에게 보낸 전체 프롬프트:\n{}", fullPrompt);
            
            // AI 요청 생성 및 호출
            GeminiRequest request = new GeminiRequest(fullPrompt);
            
            GeminiResponse aiResponse = geminiClient.generateContent(model, geminiApiKey, request);
            String aiResponseText = aiResponse.getCandidates().get(0).getContent().getParts().get(0).getText();
            
            log.info("AI 전체 응답 객체: {}", aiResponse);
            log.info("AI 파라미터 준비 응답 텍스트: {}", aiResponseText);
            
            // 필수 파라미터 개수 확인 및 로깅
            long requiredParamCount = apiInfo.getParameters().stream()
                    .filter(param -> !"OUTPUT".equals(param.getParamType()) && param.isNecessary())
                    .count();
            log.info("이 API의 필수 파라미터 개수: {}", requiredParamCount);
            
            apiInfo.getParameters().stream()
                    .filter(param -> !"OUTPUT".equals(param.getParamType()))
                    .forEach(param -> log.info("파라미터: {} (타입: {}, {})", param.getParamName(), 
                            param.getParamType(), param.isNecessary() ? "필수" : "선택사항"));
            
            // AI 응답을 JSON으로 파싱하여 requestBody 생성
            Map<String, Object> requestBody = new HashMap<>();
            try {
                // 마크다운 코드 블록 제거 (```json ... ``` 형태)
                String cleanedResponse = aiResponseText.trim();
                if (cleanedResponse.startsWith("```json")) {
                    cleanedResponse = cleanedResponse.substring(7); // "```json" 제거
                }
                if (cleanedResponse.startsWith("```")) {
                    cleanedResponse = cleanedResponse.substring(3); // "```" 제거
                }
                if (cleanedResponse.endsWith("```")) {
                    cleanedResponse = cleanedResponse.substring(0, cleanedResponse.length() - 3); // 마지막 "```" 제거
                }
                cleanedResponse = cleanedResponse.trim();
                
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>> typeRef = 
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {};
                requestBody = mapper.readValue(cleanedResponse, typeRef);
                log.info("AI가 준비한 파라미터: {}", requestBody);
                
                // 필수 파라미터 누락 검사
                for (ApiParameterDto param : apiInfo.getParameters()) {
                    if (!"OUTPUT".equals(param.getParamType()) && param.isNecessary()) {
                        if (!requestBody.containsKey(param.getParamName())) {
                            log.error("필수 파라미터 누락: {}", param.getParamName());
                        } else {
                            log.info("필수 파라미터 확인됨: {} = {}", param.getParamName(), requestBody.get(param.getParamName()));
                        }
                    }
                }
            } catch (Exception e) {
                log.error("AI 응답 파싱 실패, 빈 파라미터로 API 호출 시도: {}", e.getMessage());
                // 자연어 쿼리에서는 구조화된 폴백이 불가능하므로 빈 파라미터로 시도
                // 실제 운영에서는 더 정교한 에러 처리나 재시도 로직이 필요할 수 있음
                requestBody = new HashMap<>();
                log.warn("API {}에 빈 파라미터로 요청을 시도합니다.", apiInfo.getApiName());
            }

            // 3-2. apiInfo에서 직접 endpoint를 사용하고 HTTP 메서드에 따라 호출
            String targetUrl = apiInfo.getEndpoint();
            if (targetUrl == null || targetUrl.trim().isEmpty()) {
                log.error("{}({})에 해당하는 endpoint가 없습니다. 오케스트레이션을 중단합니다.", apiInfo.getApiName(), apiInfo.getApiId());
                throw new IllegalStateException("Endpoint for API " + apiInfo.getApiName() + " (" + apiInfo.getApiId() + ") not found.");
            }
            
            URI targetUri = new URI(targetUrl);
            String httpMethod = apiInfo.getHttpMethod();
            Map<String, Object> apiResult;
            
            // HTTP 메서드에 따라 적절한 클라이언트 메서드 호출
            if ("GET".equalsIgnoreCase(httpMethod)) {
                log.info("GET 메서드로 {}를 호출합니다. 파라미터: {}", targetUrl, requestBody);
                apiResult = genericApiClient.executeGet(targetUri, requestBody);
            } else {
                // 기본값은 POST (하위 호환성)
                log.info("POST 메서드로 {}를 호출합니다. 파라미터: {}", targetUrl, requestBody);
                apiResult = genericApiClient.executePost(targetUri, requestBody);
            }

            // 3-3. 호출 결과를 컨텍스트에 저장하여 다음 단계에서 사용
            executionContext.put(apiInfo.getApiId(), apiResult);
            log.info("단계 완료: {}. 결과: {}", apiInfo.getApiName(), apiResult);
        }

        // 4. 모든 단계가 끝난 후, 컨텍스트에 쌓인 결과들을 최종 결과로 반환
        log.info("커스텀 API 실행 완료. API ID: {}", customApiId);
        
        // 5. AI+ 기능이 활성화된 경우, 개인화 서비스를 통해 분석을 추가
        if (isAiPlusEnabled && !userId.equals("anonymous")) {
            log.info("AI+ 기능 활성화. 사용자(ID: {})의 분석 요구사항: {}", userId, aiPlusActive);
            try {
                Map<String, Object> personalizedResult = aiPersonalizationService.personalize(userId, executionContext, aiPlusActive);
                log.info("AI+ 분석 완료");
                return personalizedResult;
            } catch (Exception e) {
                log.warn("AI+ 분석 실패. 원본 데이터를 반환합니다. 오류: {}", e.getMessage());
            }
        }
        
        // AI+ 기능이 꺼진 경우, 모든 사용자에게 일관된 형태로 반환 ({"data": executionContext})
        Map<String, Object> result = new HashMap<>();
        result.put("data", executionContext);
        return result;
    }
}