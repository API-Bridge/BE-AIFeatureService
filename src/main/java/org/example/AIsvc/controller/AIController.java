package org.example.AIsvc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.AIsvc.dto.common.BaseResponse;
import org.example.AIsvc.dto.request.AnalyzeQueryRequest;
import org.example.AIsvc.dto.response.AnalyzeQueryResponse;
import org.example.AIsvc.service.AIService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import org.example.AIsvc.service.AIOrchestrationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;


import org.springframework.security.core.annotation.AuthenticationPrincipal; // 현재 인증된 사용자 정보를 주입받기 위한 어노테이션
import org.springframework.security.oauth2.jwt.Jwt;

@Tag(name = "AI Service", description = "AI 기반 API 분석 및 생성 서비스")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AIController {
    
    private final AIService aiService;
    private final AIOrchestrationService aiOrchestrationService;

    @Operation(summary = "자연어 쿼리 분석 및 API 생성 요청", description = "사용자의 자연어 쿼리를 분석하여 API 생성 프로세스를 시작합니다.")
    @PostMapping("/analyze-query")
    // 1. 메소드의 반환 타입을 실제 데이터가 포함된 형태로 변경
    public ResponseEntity<BaseResponse<AnalyzeQueryResponse>> analyzeQuery(
            // Spring Security가 현재 요청의 인증 토큰(JWT) 정보를 파싱하여 Jwt 객체로 주입 (Optional)
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AnalyzeQueryRequest request) {

        // JWT 토큰에서 사용자의 고유 식별자인 'subject'를 추출. 토큰이 없으면 "anonymous" 사용
        String userId = jwt != null ? jwt.getSubject() : "anonymous";

        // 서비스로부터 실제 데이터가 담긴 응답 객체를 받음
        AnalyzeQueryResponse responseData = aiService.analyzeAndInitiateCreation(request, userId);

        // 표준화된 성공 응답 형식으로 데이터를 감쌈
        BaseResponse<AnalyzeQueryResponse> response = BaseResponse.success(responseData, "API 생성 요청이 성공적으로 분석되어 전달되었습니다.");
        // ResponseEntity를 사용하여 HTTP 상태 코드 202 (Accepted)와 함께 '데이터가 담긴' 응답을 반환
        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }

    // 커스텀 API 실행을 위한 엔드포인트
    @Operation(summary = "생성된 커스텀 API 실행", description = "ID와 쿼리 파라미터를 사용하여 생성된 커스텀 API를 실행합니다.")
    @GetMapping("/execute/{customApiId}")
    public ResponseEntity<BaseResponse<Object>> executeCustomApi(
            @PathVariable String customApiId,
            @RequestParam String query) {

        Object result = aiOrchestrationService.executeCustomApi(customApiId, query);

        return ResponseEntity.ok(BaseResponse.success(result));
    }
}