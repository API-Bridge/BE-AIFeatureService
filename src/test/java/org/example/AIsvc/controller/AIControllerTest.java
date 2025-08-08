package org.example.AIsvc.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.AIsvc.dto.request.AnalyzeQueryRequest;
import org.example.AIsvc.service.AIService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean; // 3.4.0 부터 중단, 하지만 테스트 용도라 지장 없음.
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;

@WebMvcTest(AIController.class)
class AIControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AIService aiService;

    @WithMockUser
    @Test
    @DisplayName("TDD 1단계: 쿼리 분석 API 엔드포인트 기본 호출 테스트")
    void analyzeQuery_EndpointExists() throws Exception {
        // given
        AnalyzeQueryRequest request = new AnalyzeQueryRequest("아무 쿼리", null, false);

        // when & then
        mockMvc.perform(post("/ai/analyze-query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isAccepted());
    }

    @WithMockUser
    @Test
    @DisplayName("TDD 2단계: 쿼리 내용이 비어있을 때 400 에러 반환 테스트")
    void analyzeQuery_Fail_WithEmptyQuery() throws Exception {
        // given
        AnalyzeQueryRequest request = new AnalyzeQueryRequest("", "test-api", false);

        // when & then
        mockMvc.perform(post("/ai/analyze-query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }
}