//package org.example.AIsvc.controller;
//
//import lombok.RequiredArgsConstructor;
//import org.example.AIsvc.dto.common.BaseResponse;
//import org.example.AIsvc.dto.api_management.ApiUrlRequest;
//import org.example.AIsvc.service.AIUsageService;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.Map;
//
//@RestController
//@RequestMapping("/internal/ai")
//@RequiredArgsConstructor
//public class AIInternalController {
//
//    private final AIUsageService aiUsageService;
//
//    /**
//     * 다른 서비스(e.g., 커스텀API 서비스)로부터 API ID 목록을 받아, todo: 이거 왜있는거야??
//     * 각 ID에 해당하는 실제 URL로 변환하여 반환하는 내부 API
//     */
//    @PostMapping("/resolve-urls")
//    public ResponseEntity<BaseResponse<Map<String, String>>> resolveApiUrls(
//            @RequestBody ApiUrlRequest request) {
//
//        Map<String, String> resolvedUrls = aiUsageService.resolveApiUrls(request.getApiIds());
//
//        return ResponseEntity.ok(BaseResponse.success(resolvedUrls));
//    }
//}