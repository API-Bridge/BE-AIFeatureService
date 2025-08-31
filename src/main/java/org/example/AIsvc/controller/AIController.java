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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.RequestHeader;
import org.example.AIsvc.event.publisher.EventPublisher;
import org.example.AIsvc.event.model.CustomApiCalledEvent;
import org.example.AIsvc.client.UserApiClient;

@Tag(name = "AI Service", description = "AI 기반 API 분석 및 생성 서비스")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;
    private final AIOrchestrationService aiOrchestrationService;
    private final EventPublisher eventPublisher;
    private final UserApiClient userApiClient;

    @Operation(summary = "자연어 쿼리 분석 및 API 생성 요청", description = "사용자의 자연어 쿼리를 분석하여 API 생성 프로세스를 시작합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "API 생성 요청이 성공적으로 접수됨"),
            @ApiResponse(responseCode = "400", description = "요청 본문이 유효하지 않음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류 발생")
    })
    @PostMapping("/analyze-query")
    public ResponseEntity<BaseResponse<AnalyzeQueryResponse>> analyzeQuery(
            @Parameter(description = "사용자 ID", required = false)
            @RequestHeader(value = "X-User-Id", defaultValue = "test-user") String userId,
            @Parameter(description = "분석할 자연어 쿼리와 커스텀 API 정보를 담은 요청 본문", required = true)
            @Valid @RequestBody AnalyzeQueryRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        
        // 디버깅용 로그
        System.out.println("Content-Type: " + httpRequest.getContentType());
        System.out.println("Request received - Query: " + request.getQuery() + ", API ID: " + request.getCustomApiId());

        // 서비스로부터 실제 데이터가 담긴 응답 객체를 받음
        AnalyzeQueryResponse responseData = aiService.analyzeAndInitiateCreation(request, userId);

        // 표준화된 성공 응답 형식으로 데이터를 감쌈
        BaseResponse<AnalyzeQueryResponse> response = BaseResponse.success(responseData, "API 생성 요청이 성공적으로 분석되어 전달되었습니다.");
        // ResponseEntity를 사용하여 HTTP 상태 코드 202 (Accepted)와 함께 '데이터가 담긴' 응답을 반환
        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }

    // 커스텀 API 실행을 위한 엔드포인트
    @Operation(summary = "생성된 커스텀 API 실행", description = "ID와 쿼리 파라미터를 사용하여 생성된 커스텀 API를 실행합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "API 실행 성공"),
            @ApiResponse(responseCode = "404", description = "해당 ID의 커스텀 API를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "API 실행 중 서버 내부 오류 발생")
    })
    @GetMapping("/execute/{customApiId}")
    public ResponseEntity<BaseResponse<Object>> executeCustomApi(
            @Parameter(description = "사용자 ID", required = false)
            @RequestHeader(value = "X-User-Id", defaultValue = "user-456") String userId,
            @Parameter(description = "실행할 커스텀 API의 고유 ID", required = true)
            @PathVariable String customApiId,
            @Parameter(description = "API 실행에 필요한 파라미터 (예: location=서울&category=맛집)", required = true)
            @RequestParam String query,
            @Parameter(description = "AI+ 기능 및 분석 요구사항 (null이면 비활성화, 값이 있으면 해당 요구사항으로 AI 분석)", required = false)
            @RequestParam(value = "ai-plus", required = false) String aiPlusActive) {

        // 커스텀 API 호출 이벤트 발행
        CustomApiCalledEvent event = new CustomApiCalledEvent(
                customApiId,
                userId,
                "ai-service"
        );
        eventPublisher.publishEvent("custom_api_events", event);

        Object result = aiOrchestrationService.executeCustomApi(customApiId, query, userId, aiPlusActive);

        return ResponseEntity.ok(BaseResponse.success(result));
    }

    // 폼 데이터 지원 엔드포인트
    @PostMapping("/analyze-query-form")
    public ResponseEntity<BaseResponse<AnalyzeQueryResponse>> analyzeQueryForm(
            @RequestHeader(value = "X-User-Id", defaultValue = "test-user") String userId,
            @RequestParam("query") String query,
            @RequestParam(value = "custom_api_id", required = false) String customApiId) {
        
        // AnalyzeQueryRequest 객체 생성
        AnalyzeQueryRequest request = new AnalyzeQueryRequest(query, customApiId);
        
        AnalyzeQueryResponse responseData = aiService.analyzeAndInitiateCreation(request, userId);
        BaseResponse<AnalyzeQueryResponse> response = BaseResponse.success(responseData, "API 생성 요청이 성공적으로 분석되어 전달되었습니다.");
        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }



    // ====================== BYOK 테스트용 엔드포인트 =====================
    @Operation(summary = "사용자 BYOK (API 키) 조회 테스트", description = "지정된 사용자 ID로 User 서비스에서 BYOK 정보를 조회하는 테스트 엔드포인트입니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "BYOK 조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "User 서비스 호출 실패")
    })
    @GetMapping("/test/byok")
    public ResponseEntity<BaseResponse<Object>> testGetUserByok(
            @Parameter(description = "조회할 사용자 ID", required = true)
            @RequestParam String userId) {
        
        try {
            // UserApiClient를 통해 사용자의 BYOK 정보 조회
            org.example.AIsvc.dto.user.UserSecretResponse userSecret = 
                userApiClient.getUserSecret(userId);
            
            if (userSecret != null) {
                // 성공적으로 조회된 경우
                java.util.Map<String, Object> result = new java.util.HashMap<>();
                result.put("userId", userId);
                result.put("secretValue", userSecret.getSecretValue());
                result.put("arnId", userSecret.getArnId());
                result.put("description", userSecret.getDescription());
                result.put("secretName", userSecret.getSecretName());
                result.put("hasApiKey", userSecret.getSecretValue() != null && !userSecret.getSecretValue().isEmpty());
                result.put("status", "SUCCESS");
                result.put("message", "BYOK 정보 조회 성공");
                
                // 기존 필드도 호환성을 위해 추가
                result.put("arn", userSecret.getArn());
                result.put("arnDescription", userSecret.getArnDescription());
                
                return ResponseEntity.ok(BaseResponse.success(result));
            } else {
                // Fallback이 실행되거나 서비스 호출 실패
                java.util.Map<String, Object> result = new java.util.HashMap<>();
                result.put("userId", userId);
                result.put("secretValue", null);
                result.put("arnId", null);
                result.put("description", null);
                result.put("secretName", null);
                result.put("hasApiKey", false);
                result.put("status", "FALLBACK");
                result.put("message", "User 서비스 호출 실패 또는 Fallback 실행됨");
                
                // 기존 필드도 호환성을 위해 추가
                result.put("arn", null);
                result.put("arnDescription", null);
                
                return ResponseEntity.ok(BaseResponse.success(result));
            }
            
        } catch (Exception e) {
            // 예외 발생 시
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("userId", userId);
            result.put("error", e.getClass().getSimpleName());
            result.put("errorMessage", e.getMessage());
            result.put("status", "ERROR");
            result.put("message", "BYOK 조회 중 오류 발생");
            
            BaseResponse<Object> errorResponse = BaseResponse.error("BYOK 조회 실패: " + e.getMessage());
            errorResponse.setData(result);
            return ResponseEntity.status(500).body(errorResponse);
        }
    }


}