package org.example.AIsvc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.AIsvc.client.CustomApiClient;
import org.example.AIsvc.client.GeminiClient;
import org.example.AIsvc.dto.custom_api.InitiateCreationRequest;
import org.example.AIsvc.dto.gemini.GeminiRequest;
import org.example.AIsvc.dto.gemini.GeminiResponse;
import org.example.AIsvc.dto.request.AnalyzeQueryRequest;
import org.example.AIsvc.dto.response.AnalyzeQueryResponse;
import org.example.AIsvc.dto.response.AnalyzeQueryResponse.AnalysisResultDto;
import org.example.AIsvc.enums.ApiDomain;
import org.example.AIsvc.enums.ApiKeyword;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.core.env.Environment;
import org.example.AIsvc.event.model.ApiAnalysisEvent;
import org.example.AIsvc.event.publisher.EventPublisher;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {

    private final GeminiClient geminiClient;
    private final LLMResponseParser llmResponseParser;
    private final CustomApiClient customApiClient;
    private final Environment environment;
    private final ObjectMapper objectMapper;
    private final EventPublisher eventPublisher;

    @Value("${gemini.api.key}")
    private String geminiApiKey; // Gemini API 키
    @Value("${gemini.model}")
    private String model; // 사용할 Gemini 모델 이름

    @Override
    public AnalyzeQueryResponse analyzeAndInitiateCreation(AnalyzeQueryRequest request, String userId) {

        log.info("사용자 ID '{}'의 쿼리 분석 시작: {}", userId, request.getQuery());

        // 1. LLM에 전달할 프롬프트를 생성
        String prompt = createPromptForAnalysis(request.getQuery());

        // 2. Gemini API 요청 DTO를 생성
        GeminiRequest geminiRequest = new GeminiRequest(prompt);

        // 3. Feign 클라이언트를 통해 Gemini API를 실제로 호출
        GeminiResponse geminiResponse = geminiClient.generateContent(model, geminiApiKey, geminiRequest);

        // 4. LLM 응답에서 실제 텍스트 내용을 추출
        // Gemini 응답 구조가 복잡, 여러 계층을 거쳐 실제 텍스트를 가져옴
        String llmResponseJson = geminiResponse.getCandidates().get(0).getContent().getParts().get(0).getText();


        // 5. 로그로 LLM의 원본 응답을 기록. 디버깅에 유용
        log.info("LLM 원본 응답 수신: {}", llmResponseJson);

        // 6. 주입받은 파서를 사용하여 JSON 응답을 파싱
        AnalysisResultDto analysisResult = llmResponseParser.parse(llmResponseJson);

        // 7. dev 프로필에서는 CustomAPI 호출을 건너뛰고, prod에서는 정상 호출
        if (!environment.acceptsProfiles(org.springframework.core.env.Profiles.of("dev"))) {
            // 8. CustomApiClient로 보낼 요청 DTO를 생성
            InitiateCreationRequest creationRequest = InitiateCreationRequest.builder()
                    .userId(userId) // 컨트롤러에서 전달받은 userId
                    .customApiId(request.getCustomApiId()) // 기존 요청에 있던 customApiId
                    .domains(analysisResult.getDetectedDomains()) // 파싱된 도메인 리스트
                    .keywords(analysisResult.getDetectedKeywords()) // 파싱된 키워드 리스트
                    .userQuery(request.getQuery()) // 사용자 입력 쿼리 (originalQuery -> userQuery로 변경)
                    .aiPlusActive(false) // 기본값으로 false 설정
                    .build();

            // 9. CustomApiClient를 호출하여 다음 서비스로 작업을 전달
            // CustomApiClient를 호출하기 직전에, 보낼 객체를 JSON 문자열로 변환하여 로그로 출력
            try {
                log.info("Sending to CustomApiSvc -> \n{}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(creationRequest));
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize creationRequest", e);
            }
            customApiClient.initiateCreation(creationRequest);
            log.info("CustomAPI 관리 서비스로 생성 요청 전달 완료. Custom API ID: {}", request.getCustomApiId());
        } else {
            log.info("개발 환경에서는 CustomAPI 호출을 건너뜁니다. Custom API ID: {}", request.getCustomApiId());
        }

        // 10. Kafka로 보낼 이벤트 객체를 생성
        // 분석 과정에서 얻은 모든 주요 정보를 담음
        ApiAnalysisEvent event = new ApiAnalysisEvent(
                userId,
                request.getCustomApiId(),
                analysisResult.getDetectedDomains(),
                analysisResult.getDetectedKeywords(),
                model // 사용된 LLM 모델 정보도 함께 기록
        );

        // 11. EventPublisher를 통해 'ai-analysis-logs'라는 토픽으로 이벤트를 발행
        // 비동기적으로 처리, API 응답 시간을 지연시키지 않음
        eventPublisher.publishEvent("ai-analysis-logs", event);

        // 12. 최종 응답 객체에 파싱된 analysisResult를 포함하여 반환
        return AnalyzeQueryResponse.builder()
                .status("ACCEPTED")
                .message("API 생성 요청이 성공적으로 분석되어 전달되었습니다.")
                .requestedApiId(request.getCustomApiId())
                .analysisResult(analysisResult)
                .build();
    }

    /**
     * LLM에 전달할 프롬프트를 생성하는 내부 메소드
     * @param query 사용자 원본 쿼리
     * @return LLM에 최적화된 프롬프트 문자열
     */
    private String createPromptForAnalysis(String query) {
        // 프롬프트 엔지니어링: 더 명확하고 강력하게 JSON 형식만을 요구하도록 수정
        return String.format(
                "사용자 쿼리를 분석하여 가장 적합한 도메인과 키워드를 JSON 형식으로만 응답해줘. " +
                        "다른 설명이나 Markdown 코드 블록 없이, 오직 JSON 객체 자체만 반환해야 해. " +
                        "사용 가능한 도메인 목록: [%s]. " +
                        "사용 가능한 키워드 목록: [%s]. " +
                        "쿼리: \"%s\"",
                getAvailableDomains(),
                getAvailableKeywords(),
                query
        );
    }

    // 모든 ApiDomain 코드를 콤마로 구분된 문자열로 만드는 유틸리티 메소드
    private String getAvailableDomains() {
        // ENUM의 모든 상수 배열을 스트림으로 변환
        return java.util.Arrays.stream(ApiDomain.values())
                .map(ApiDomain::getCode) // 각 ENUM 상수에 대해 getCode() 메소드를 호출하여 코드 문자열로 변환
                .collect(Collectors.joining(", ")); // 변환된 모든 문자열을 ", "로 연결하여 하나의 문자열로 변환
    }

    // 모든 ApiKeyword 코드를 콤마로 구분된 문자열로 만드는 유틸리티 메소드
    private String getAvailableKeywords() {
        return java.util.Arrays.stream(ApiKeyword.values())
                .map(ApiKeyword::getCode)
                .collect(Collectors.joining(", "));
    }

}