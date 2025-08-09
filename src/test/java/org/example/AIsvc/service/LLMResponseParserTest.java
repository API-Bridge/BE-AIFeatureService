package org.example.AIsvc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.example.AIsvc.dto.response.AnalyzeQueryResponse.AnalysisResultDto;
import org.example.AIsvc.enums.ApiDomain;
import org.example.AIsvc.enums.ApiKeyword;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LLMResponseParserTest {

    private LLMResponseParser llmResponseParser;

    @BeforeEach
    void setUp() {
        llmResponseParser = new LLMResponseParser();
    }

    @Test
    @DisplayName("성공: 유효한 JSON 응답을 AnalysisResultDto로 정확히 파싱한다")
    void parse_Success() throws JsonProcessingException {
        // given
        // LLM이 반환했을 법한 가상의 JSON 문자열 응답을 준비
        String llmResponseJson = "{\"domains\":[\"weather\", \"news\"], \"keywords\":[\"air_quality\", \"breaking_news\"]}";

        // when
        // llmResponseParser의 parse 메소드를 호출하여 JSON 문자열을 파싱
        AnalysisResultDto result = llmResponseParser.parse(llmResponseJson);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getDetectedDomains()).containsExactlyInAnyOrder(
                ApiDomain.WEATHER.name().toLowerCase(),
                ApiDomain.NEWS.name().toLowerCase()
        );
        assertThat(result.getDetectedKeywords()).containsExactlyInAnyOrder(
                ApiKeyword.AIR_QUALITY.name().toLowerCase(),
                ApiKeyword.BREAKING_NEWS.name().toLowerCase()
        );
    }

    @Test
    @DisplayName("실패: JSON 형식이 잘못되었을 때 null을 반환한다")
    void parse_Fail_WithInvalidJson() {
        // given
        // 중괄호가 닫히지 않은, 잘못된 형식의 JSON 문자열을 준비
        String invalidJson = "{\"domains\":[\"weather\"";

        // when
        AnalysisResultDto result = llmResponseParser.parse(invalidJson);

        // then .
        assertThat(result).isNull();
    }
}