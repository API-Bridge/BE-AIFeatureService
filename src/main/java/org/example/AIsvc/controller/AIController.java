package org.example.AIsvc.controller;

import lombok.RequiredArgsConstructor;
import org.example.AIsvc.dto.request.AnalyzeQueryRequest;
import org.example.AIsvc.service.AIService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AIController {
    
    private final AIService aiService;

    @PostMapping("/analyze-query")
    // 요청 본문을 AnalyzeQueryRequest 객체에 매핑
    public ResponseEntity<Void> analyzeQuery(@Valid @RequestBody AnalyzeQueryRequest request) {
        // 서비스 계층의 메소드를 호출
        aiService.analyzeAndInitiateCreation(request);
        // HTTP 상태 코드 202를 담은 빈 응답을 반환
        return ResponseEntity.accepted().build();
    }
}