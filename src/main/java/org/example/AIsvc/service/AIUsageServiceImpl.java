package org.example.AIsvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.AIsvc.client.ApiManagementClient;
import org.example.AIsvc.dto.api_management.ApiUrlRequest;
import org.example.AIsvc.dto.api_management.ApiUrlResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIUsageServiceImpl implements AIUsageService {

    private final ApiManagementClient apiManagementClient;

    @Override
    public Map<String, String> resolveApiUrls(List<String> apiIds) {
        log.info("API URL 조회를 시작합니다. 대상 API ID 수: {}", apiIds.size());

        // 1. ApiManagementClient로 보낼 요청 DTO 생성
        ApiUrlRequest request = new ApiUrlRequest(apiIds);
        
        // 2. ApiManagementClient를 호출하여 API 상세 정보 목록을 받아옴
        ApiUrlResponse response = apiManagementClient.getApiUrls(request);
        
        // 3. 받아온 응답(ApiUrlResponse)을 key-value 형태의 Map으로 변환
        // response.getApis().stream(): API 상세 정보 리스트를 스트림으로 변환하여 효율적인 데이터 처리를 준비
        Map<String, String> resolvedUrlMap = response.getApis().stream()
                // Collectors.toMap(): 스트림의 각 요소를 Map의 키와 값으로 변환하여 새로운 Map을 생성
                .collect(Collectors.toMap(
                        ApiUrlResponse.ApiUrlDetail::getApiId,   // Map의 Key는 ApiUrlDetail 객체의 apiId를 사용
                        ApiUrlResponse.ApiUrlDetail::getApiUrl    // Map의 Value는 ApiUrlDetail 객체의 apiUrl을 사용
                ));

        log.info("API URL 조회를 완료했습니다. 변환된 URL 수: {}", resolvedUrlMap.size());
        
        // 4. 최종적으로 변환된 Map을 반환
        return resolvedUrlMap;
    }
}