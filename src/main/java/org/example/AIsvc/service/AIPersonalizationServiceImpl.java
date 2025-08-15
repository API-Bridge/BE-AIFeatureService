package org.example.AIsvc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.AIsvc.client.GeminiClient;
import org.example.AIsvc.client.UserApiClient;
import org.example.AIsvc.dto.gemini.GeminiRequest;
import org.example.AIsvc.dto.gemini.GeminiResponse;
import org.example.AIsvc.dto.user.UserPlanResponse;
import org.example.AIsvc.dto.user.UserSecretResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIPersonalizationServiceImpl implements AIPersonalizationService {

    // 필요한 클라이언트 및 컴포넌트를 생성자를 통해 주입 받음
    private final UserApiClient userApiClient;
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    // application.yml 파일의 값을 필드에 주입
    @Value("${gemini.api.key}")
    private String systemGeminiApiKey;
    @Value("${gemini.model}")
    private String model;

    @Override
    public Map<String, Object> personalize(String userId, Object rawData) {
        try {
            // 1. 사용자 서비스에 문의하여 사용자의 구독 플랜 정보를 가져옴
            UserPlanResponse planResponse = userApiClient.getUserPlan(userId);
            String planName = planResponse.getPlanName().toUpperCase();
            log.info("사용자(ID: {}) 플랜 확인: {}", userId, planName);

            // 2. 구독 플랜에 따라 분기
            switch (planName) {
                case "FREE":
                    return personalizeForFreeUser(userId, rawData);
                case "PRO":
                    return personalizeForProUser(userId, rawData);
                default:
                    // FREE나 PRO가 아닌 다른 플랜의 경우, 개인화 없이 원본 데이터를 반환
                    log.info("사용자(ID: {})는 개인화 대상 플랜이 아니므로 건너뜁니다.", userId);
                    return Map.of("data", rawData);
            }
        } catch (Exception e) {
            log.error("사용자(ID: {}) 개인화 처리 중 오류 발생. 원본 데이터를 반환합니다.", userId, e);
            return convertToMap(rawData);
        }
    }

    // 무료 사용자를 위한 개인화 로직을 처리하는 private 메소드
    private Map<String, Object> personalizeForFreeUser(String userId, Object rawData) throws JsonProcessingException {
        // 사용자의 BYOK 키(ARN) 정보를 조회
        UserSecretResponse userSecret = userApiClient.getUserSecret(userId);
        if (userSecret == null || userSecret.getArn() == null) {
            log.warn("무료 사용자(ID: {})의 BYOK 키를 찾을 수 없어 개인화를 건너뜁니다.", userId);
            return convertToMap(rawData);
        }
        
        // TODO: 실제로는 ARN으로 AWS Secrets Manager에서 키를 조회해야 합니다. 시스템 키를 임시로 사용
        String apiKey = systemGeminiApiKey;
        log.info("무료 사용자(ID: {}) BYOK(ARN: {})를 사용하여 개인화 시작", userId, userSecret.getArn());

        // LLM에 간단한 요약을 요청하는 프롬프트를 생성
        String prompt = String.format("다음 JSON 데이터를 보고, 사용자에게 친절한 요약 문장을 한 문장으로 만들어줘. 데이터: %s", objectMapper.writeValueAsString(rawData));
        
        // LLM을 호출하고, 응답을 받아옴
        String summary = callGemini(apiKey, prompt);

        // 원본 데이터에 'summary' 필드를 추가하여 반환
        Map<String, Object> finalResult = new HashMap<>();
        finalResult.put("summary", summary); // AI가 생성한 요약 정보
        finalResult.put("data", rawData); // 원본 데이터는 'data' 키 아래에 배치
        return finalResult;
    }

    // 프로 사용자를 위한 개인화 로직을 처리하는 private 메소드
    private Map<String, Object> personalizeForProUser(String userId, Object rawData) throws Exception {
        log.info("프로 사용자(ID: {}) 시스템 AI를 사용하여 개인화 시작", userId);
        
        // LLM에 더 깊이 있는 분석(인사이트)을 요청하는 프롬프트를 생성
        String rawDataJson = objectMapper.writeValueAsString(rawData);
        String prompt = String.format(
                "다음 JSON 데이터를 분석해서, 아래 형식에 맞춰 응답해줘. 다른 설명 없이 JSON 객체만 반환해야 해." +
                        "{\"summary\": \"데이터에 대한 간단한 요약\", \"insights\": [\"전문가 관점의 흥미로운 인사이트 1\", \"인사이트 2\"], \"predictions\": \"데이터 기반의 간단한 예측\"}" +
                        "데이터: %s",
                rawDataJson
        );
        // 프로 사용자는 시스템의 API 키를 사용하여 LLM을 호출
        String llmResultJson = callGemini(systemGeminiApiKey, prompt);

        // LLM이 반환한 JSON 문자열을 Map 객체로 파싱합니다.
        Map<String, Object> personalizedData = objectMapper.readValue(llmResultJson, new TypeReference<>() {});

        // 최종 응답 구조를 예시에 맞게 조합
        Map<String, Object> finalResult = new HashMap<>();
        finalResult.put("summary", personalizedData.get("summary")); // LLM이 만든 summary
        finalResult.put("insights", personalizedData.get("insights")); // LLM이 만든 insights
        finalResult.put("predictions", personalizedData.get("predictions")); // LLM이 만든 predictions
        finalResult.put("data", rawData); // 원본 데이터는 'data' 키 아래에 배치
        return finalResult;
    }

    // LLM 호출 로직을 공통 메소드로 분리하여 코드 중복을 줄임
    private String callGemini(String apiKey, String prompt) {
        GeminiRequest geminiRequest = new GeminiRequest(prompt);
        GeminiResponse geminiResponse = geminiClient.generateContent(model, apiKey, geminiRequest);
        String rawText = geminiResponse.getCandidates().get(0).getContent().getParts().get(0).getText();
        return rawText.replace("```json", "").replace("```", "").trim();
    }
    
    // 어떤 타입의 rawData가 들어오든 안전하게 Map으로 변환
    private Map<String, Object> convertToMap(Object data) {
        try {
            Map<String, Object> convertedMap = objectMapper.convertValue(data, new TypeReference<>() {});
            return new HashMap<>(convertedMap);
        } catch (Exception e) {
            log.warn("데이터를 Map으로 변환할 수 없습니다. 새 Map을 생성합니다.");
            return new HashMap<>();
        }
    }
}