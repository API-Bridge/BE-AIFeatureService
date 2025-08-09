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
            // 1. JSON 문자열을 임시 DTO 객체(LlmResponseDto)로 변환(역직렬화)
            LlmResponseDto llmResponseDto = objectMapper.readValue(llmResponseJson, LlmResponseDto.class);

            // 2. 문자열 리스트를 ENUM 리스트로 변환
            // llmResponseDto.getDomains()가 null일 경우를 대비하여 안전하게 빈 리스트로 처리
            List<ApiDomain> domains = (llmResponseDto.getDomains() == null) ? Collections.emptyList() :
                    llmResponseDto.getDomains().stream() // 문자열 리스트를 스트림(Stream)으로 변환하여 처리 준비
                            .map(ApiDomain::fromCode) // 각 문자열에 대해 ApiDomain.fromCode 메소드를 호출하여 ENUM으로 매핑
                            .filter(Objects::nonNull) // fromCode 결과가 null인 경우(알 수 없는 도메인)는 걸러냄
                            .collect(Collectors.toList()); // 변환된 ENUM들을 다시 리스트로 수집

            List<ApiKeyword> keywords = (llmResponseDto.getKeywords() == null) ? Collections.emptyList() :
                    llmResponseDto.getKeywords().stream()
                            .map(ApiKeyword::fromCode)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

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
                    .priority("parallel") // 우선순위는 현재 하드코딩. 향후 LLM이 결정하도록 확장 가능.
                    .build();

        } catch (JsonProcessingException e) {
            log.error("LLM 응답 JSON 파싱 실패: {}", llmResponseJson, e);
            return null;
        }
    }

    @Getter
    private static class LlmResponseDto {
        private List<String> domains;
        private List<String> keywords;
    }
}