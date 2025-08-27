package org.example.AIsvc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.example.AIsvc.dto.response.AnalyzeQueryResponse.AnalysisResultDto;
import org.example.AIsvc.enums.ApiDomain;
import org.example.AIsvc.enums.ApiKeyword;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
public class LLMResponseParser {

    private final ObjectMapper objectMapper;

    public LLMResponseParser() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * LLM이 반환한 JSON 문자열을 파싱하여 AnalysisResultDto 객체로 변환
     * @param llmResponseJson LLM으로부터 받은 JSON 형식의 응답 문자열
     * @return 파싱된 AnalysisResultDto 객체. 파싱 실패 시 null 반환.
     */
    public AnalysisResultDto parse(String llmResponseJson) {
        try {
            // 0. 마크다운 형식에서 순수 JSON 추출
            String cleanJsonString = extractJsonFromMarkdown(llmResponseJson);
            
            // 1. JSON 문자열을 임시 DTO 객체(LlmResponseDto)로 변환
            LlmResponseDto llmResponseDto = objectMapper.readValue(cleanJsonString, LlmResponseDto.class);

            // 2. 문자열 리스트를 ENUM 리스트로 변환
            // llmResponseDto.getDomains()가 null일 경우를 대비하여 안전하게 빈 리스트로 처리
            List<ApiDomain> domains = (llmResponseDto.getDomains() == null) ? Collections.emptyList() :
                    llmResponseDto.getDomains().stream() // 문자열 리스트를 스트림(Stream)으로 변환하여 처리 준비
                            .map(ApiDomain::fromCode) // 각 문자열에 대해 ApiDomain.fromCode 메소드를 호출하여 ENUM으로 매핑
                            .filter(Objects::nonNull) // fromCode 결과가 null인 경우(알 수 없는 도메인)는 걸러냄
                            .toList(); // 변환된 ENUM들을 다시 리스트로 수집

            List<ApiKeyword> keywords = (llmResponseDto.getKeywords() == null) ? Collections.emptyList() :
                    llmResponseDto.getKeywords().stream()
                            .map(ApiKeyword::fromCode)
                            .filter(Objects::nonNull)
                            .toList();

            // 3. ENUM을 문자열로 변환
            List<String> domainStrings = domains.stream()
                    .map(ApiDomain::getCode)
                    .collect(Collectors.toList());

            List<String> keywordStrings = keywords.stream()
                    .map(ApiKeyword::getCode)
                    .collect(Collectors.toList());

            // 4. 최종 AnalysisResultDto 객체를 빌더 패턴으로 생성하여 반환
            return AnalysisResultDto.builder()
                    .detectedDomains(domainStrings)
                    .detectedKeywords(keywordStrings)
                    .build();

        } catch (JsonProcessingException e) {
            log.error("LLM 응답 JSON 파싱 실패: {}", llmResponseJson, e);
            return null;
        }
    }

    /**
     * 마크다운 형식(```json)에서 순수 JSON 문자열 추출
     * @param response LLM으로부터 받은 원본 응답
     * @return 순수 JSON 문자열
     */
    private String extractJsonFromMarkdown(String response) {
        if (response == null) {
            return "";
        }
        
        // 마크다운 코드 블록 제거
        String cleaned = response.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        }
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        
        return cleaned.trim();
    }

    @Getter
    private static class LlmResponseDto {
        private List<String> domains;
        private String domain; // 단수형 문자열
        private List<String> keywords;
        
        // domains가 null이면 domain을 리스트로 변환해서 반환
        public List<String> getDomains() {
            if (domains != null) {
                return domains;
            }
            if (domain != null) {
                return Collections.singletonList(domain);
            }
            return Collections.emptyList();
        }
    }
}