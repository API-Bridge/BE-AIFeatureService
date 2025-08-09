package org.example.AIsvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.AIsvc.client.GeminiClient;
import org.example.AIsvc.dto.gemini.GeminiRequest;
import org.example.AIsvc.dto.gemini.GeminiResponse;
import org.example.AIsvc.dto.request.AnalyzeQueryRequest;
import org.example.AIsvc.dto.response.AnalyzeQueryResponse;
import org.example.AIsvc.dto.response.AnalyzeQueryResponse.AnalysisResultDto;
import org.example.AIsvc.enums.ApiDomain;
import org.example.AIsvc.enums.ApiKeyword;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {

    private final GeminiClient geminiClient; // Gemini API와 통신하는 클라이언트
    private final LLMResponseParser llmResponseParser; // LLM 응답을 파싱하는 컴포넌트

    @Value("${gemini.api.key}")
    private String geminiApiKey;
    @Value("${gemini.model}")
    private String model;

    @Override
    public AnalyzeQueryResponse analyzeAndInitiateCreation(AnalyzeQueryRequest request) {
        // 서비스 로직의 시작을 알리는 로그를 기록. 중괄호({})는 파라미터를 안전하게 삽입
        log.info("쿼리 분석 시작: {}", request.getQuery());

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

        // TODO: Day 4에서 이 analysisResult를 CustomApiSvc로 전달하는 로직을 구현합니다.

        // 7. 최종 응답 객체에 파싱된 analysisResult를 포함하여 반환
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

    private String getAvailableDomains() {
        return java.util.Arrays.stream(ApiDomain.values())
                .map(ApiDomain::getCode)
                .collect(Collectors.joining(", "));
    }

    private String getAvailableKeywords() {
        return java.util.Arrays.stream(ApiKeyword.values())
                .map(ApiKeyword::getCode)
                .collect(Collectors.joining(", "));
    }
}