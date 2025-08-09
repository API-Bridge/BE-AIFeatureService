package org.example.AIsvc.service;

// ## import: 필요한 클래스들을 가져옵니다.
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.AIsvc.client.GeminiClient;
import org.example.AIsvc.dto.gemini.GeminiRequest;
import org.example.AIsvc.dto.gemini.GeminiResponse;
import org.example.AIsvc.dto.request.AnalyzeQueryRequest;
import org.example.AIsvc.dto.response.AnalyzeQueryResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {

    private final GeminiClient geminiClient;

    // ## @Value: application.yml 파일의 값을 필드에 주입합니다.
    @Value("${gemini.api.key}")
    private String geminiApiKey;
    @Value("${gemini.model}")
    private String model;

    @Override
    public AnalyzeQueryResponse analyzeAndInitiateCreation(AnalyzeQueryRequest request) {
        log.info("쿼리 분석 시작: {}", request.getQuery());

        // 1. LLM에 보낼 프롬프트를 생성
        String prompt = "사용자 쿼리를 분석하여 가장 적합한 도메인과 키워드를 JSON 형식으로 추출해줘. 쿼리: " + request.getQuery();

        // 2. Gemini API 요청 DTO를 생성
        GeminiRequest geminiRequest = new GeminiRequest(prompt);

        // 3. Feign 클라이언트를 통해 Gemini API를 실제로 호출
        GeminiResponse geminiResponse = geminiClient.generateContent(model, geminiApiKey, geminiRequest);

        // 4. LLM의 응답을 로그로 기록
        log.info("LLM 응답 수신 완료.");

        // 5. 최종 응답 객체를 빌더 패턴으로 생성하여 반환
        return AnalyzeQueryResponse.builder()
                .status("ACCEPTED")
                .message("API 생성 요청이 성공적으로 분석되어 전달되었습니다.")
                .requestedApiId(request.getCustomApiId())
                .build();
    }
}