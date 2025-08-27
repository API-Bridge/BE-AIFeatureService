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
    public Object executeCustomApi(String customApiId, String query, String userId, boolean aiPlusEnabled) {
        log.info("커스텀 API 실행 시작. API ID: {}, 쿼리: {}, 사용자: {}, AI+: {}", customApiId, query, userId, aiPlusEnabled);

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
            promptBuilder.append("사용자의 자연어 요청을 분석해서 API 호출에 필요한 파라미터를 추출해주세요.\n\n");
            
            promptBuilder.append("호출할 API:\n");
            promptBuilder.append("- 이름: ").append(apiInfo.getApiName()).append("\n");
            promptBuilder.append("- 설명: 이 API는 다음 파라미터들을 필요로 합니다.\n");
            
            for (ApiParameterDto param : apiInfo.getParameters()) {
                if ("INPUT".equals(param.getParamType())) {
                    promptBuilder.append("  * ").append(param.getParamName())
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
            
            promptBuilder.append("\n사용자 요청을 분석해서 이 API에 필요한 파라미터 값들을 추출하고, JSON 형태로만 응답해주세요.\n");
            promptBuilder.append("만약 요청에서 특정 값을 찾을 수 없다면 합리적인 기본값이나 추론된 값을 사용하세요.\n");
            promptBuilder.append("응답 형식: {\"파라미터명\": \"값\", \"파라미터명2\": \"값2\"}");
            
            // AI 요청 생성 및 호출
            GeminiRequest request = new GeminiRequest(promptBuilder.toString());
            
            GeminiResponse aiResponse = geminiClient.generateContent(model, geminiApiKey, request);
            String aiResponseText = aiResponse.getCandidates().get(0).getContent().getParts().get(0).getText();
            
            log.info("AI 파라미터 준비 응답: {}", aiResponseText);
            
            // AI 응답을 JSON으로 파싱하여 requestBody 생성
            Map<String, Object> requestBody = new HashMap<>();
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>> typeRef = 
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {};
                requestBody = mapper.readValue(aiResponseText.trim(), typeRef);
                log.info("AI가 준비한 파라미터: {}", requestBody);
            } catch (Exception e) {
                log.error("AI 응답 파싱 실패, 빈 파라미터로 API 호출 시도: {}", e.getMessage());
                // 자연어 쿼리에서는 구조화된 폴백이 불가능하므로 빈 파라미터로 시도
                // 실제 운영에서는 더 정교한 에러 처리나 재시도 로직이 필요할 수 있음
                requestBody = new HashMap<>();
                log.warn("API {}에 빈 파라미터로 요청을 시도합니다.", apiInfo.getApiName());
            }

            // 3-2. apiInfo에서 직접 endpoint를 사용
            String targetUrl = apiInfo.getEndpoint();
            if (targetUrl == null || targetUrl.trim().isEmpty()) {
                log.error("{}({})에 해당하는 endpoint가 없습니다. 오케스트레이션을 중단합니다.", apiInfo.getApiName(), apiInfo.getApiId());
                throw new IllegalStateException("Endpoint for API " + apiInfo.getApiName() + " (" + apiInfo.getApiId() + ") not found.");
            }
            URI targetUri = new URI(targetUrl);
            Map<String, Object> apiResult = genericApiClient.executePost(targetUri, requestBody);

            // 3-3. 호출 결과를 컨텍스트에 저장하여 다음 단계에서 사용
            executionContext.put(apiInfo.getApiId(), apiResult);
            log.info("단계 완료: {}. 결과: {}", apiInfo.getApiName(), apiResult);
        }

        // 4. 모든 단계가 끝난 후, 컨텍스트에 쌓인 결과들을 최종 결과로 반환
        log.info("커스텀 API 실행 완료. API ID: {}", customApiId);
        
        // 5. AI+ 기능이 활성화된 경우, 개인화 서비스를 통해 서머리를 추가
        if (aiPlusEnabled && !userId.equals("anonymous")) {
            log.info("AI+ 기능 활성화. 사용자(ID: {})의 개인 AI 키로 서머리를 생성합니다.", userId);
            try {
                Map<String, Object> personalizedResult = aiPersonalizationService.personalize(userId, executionContext);
                log.info("AI+ 서머리 생성 완료");
                return personalizedResult;
            } catch (Exception e) {
                log.warn("AI+ 서머리 생성 실패. 원본 데이터를 반환합니다. 오류: {}", e.getMessage());
            }
        }
        
        // AI+ 기능이 꺼진 경우, 모든 사용자에게 일관된 형태로 반환 ({"data": executionContext})
        Map<String, Object> result = new HashMap<>();
        result.put("data", executionContext);
        return result;
    }
}