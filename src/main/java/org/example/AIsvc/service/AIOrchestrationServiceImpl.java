package org.example.AIsvc.service;

// ## import: 필요한 클래스들을 가져옵니다.
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows; // ## Lombok: 체크 예외(checked exception)를 언체크 예외로 변환하여 코드를 간결하게 만듭니다.
import lombok.extern.slf4j.Slf4j;
import org.example.AIsvc.client.CustomApiClient;
import org.example.AIsvc.client.GenericApiClient;
import org.example.AIsvc.dto.execution.ApiParameterDto;
import org.example.AIsvc.dto.execution.CustomApiResponseDto;
import org.example.AIsvc.dto.execution.ExternalApiInfoDto;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder; // ## URL 쿼리 파라미터를 파싱하기 위한 유틸리티
import org.example.AIsvc.client.ApiManagementClient;
import org.example.AIsvc.dto.api_management.ApiUrlRequest;
import org.example.AIsvc.dto.api_management.ApiUrlResponse;
import org.example.AIsvc.service.AIPersonalizationService;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIOrchestrationServiceImpl implements AIOrchestrationService {

    private final CustomApiClient customApiClient;
    private final GenericApiClient genericApiClient;
    private final ApiManagementClient apiManagementClient;
    private final AIPersonalizationService aiPersonalizationService;

    @SneakyThrows
    @Override
    public Object executeCustomApi(String customApiId, String query, String userId, boolean aiPlusEnabled) {
        log.info("커스텀 API 실행 시작. API ID: {}, 쿼리: {}, 사용자: {}, AI+: {}", customApiId, query, userId, aiPlusEnabled);

        //  1. `커스텀API 서비스`를 호출하여 API 실행 계획('레시피')을 가져옴
        CustomApiResponseDto recipe = customApiClient.getApiDetails(customApiId);

        // 2. 레시피에 있는 모든 API ID들을 추출하여 리스트로 생성
        List<String> apiIdsToResolve = recipe.getExternalApiInfoList().stream()
                .map(ExternalApiInfoDto::getApiId)
                .collect(Collectors.toList());

        // 3. `API관리 서비스`에 API ID 리스트를 보내 실제 URL이 담긴 Map을 받아옴
        ApiUrlResponse apiUrlResponse = apiManagementClient.getApiUrls(new ApiUrlRequest(apiIdsToResolve));
        Map<String, String> apiUrlMap = apiUrlResponse.getApis().stream()
                .collect(Collectors.toMap(
                        ApiUrlResponse.ApiUrlDetail::getApiId,
                        ApiUrlResponse.ApiUrlDetail::getApiUrl
                ));
        log.info("{}개의 API URL 조회를 완료했습니다.", apiUrlMap.size());

        // 2. 모든 API 호출 결과를 저장하고, 다음 API의 입력으로 사용될 데이터 저장소(컨텍스트)를 생성
        Map<String, Object> executionContext = new HashMap<>();

        // 최초 사용자 쿼리를 컨텍스트에 추가 (예: "location=서울" -> key: "location", value: "서울")
        Map<String, String> initialParams = UriComponentsBuilder.newInstance().query(query).build().getQueryParams().toSingleValueMap();
        executionContext.put("INITIAL_REQUEST", initialParams);

        // 3. 적힌 순서대로 외부 API를 하나씩 호출
        for (ExternalApiInfoDto apiInfo : recipe.getExternalApiInfoList()) {
            log.info("단계 실행: {}", apiInfo.getApiName());

            // 3-1. 이번 단계 API 호출에 필요한 입력 파라미터를 컨텍스트에서 찾아 준비
            Map<String, Object> requestBody = new HashMap<>();
            for (ApiParameterDto param : apiInfo.getParameters()) {
                // 파라미터가 'INPUT' 타입일 경우에만 처리
                if ("INPUT".equals(param.getParamType())) {
                    if (initialParams.containsKey(param.getParamName())) {
                        requestBody.put(param.getParamName(), initialParams.get(param.getParamName()));
                    }
                }
            }

            // 3-2. apiInfo에서 직접 URL을 가져오는 대신, 조회해온 apiUrlMap에서 ID에 맞는 URL을 가져옴
            String targetUrl = apiUrlMap.get(apiInfo.getApiId());
            if (targetUrl == null) {
                log.error("{}에 해당하는 URL을 찾을 수 없습니다. 오케스트레이션을 중단합니다.", apiInfo.getApiId());
                throw new IllegalStateException("URL for API ID " + apiInfo.getApiId() + " not found.");
            }
            URI targetUri = new URI(targetUrl);
            Map<String, Object> apiResult = genericApiClient.executePost(targetUri, requestBody);

            // 3-3. 호출 결과를 컨텍스트에 저장하여 다음 단계에서 사용
            executionContext.put(apiInfo.getApiId(), apiResult);
            log.info("단계 완료: {}. 결과: {}", apiInfo.getApiName(), apiResult);
        }

        // 4. 모든 단계가 끝난 후, 컨텍스트에 쌓인 결과들을 최종 결과로 반환
        log.info("커스텀 API 실행 완료. API ID: {}", customApiId);
        
        // 5. AI+ 기능이 활성화된 경우, 개인화 서비스를 통해 서머리를 추가
        if (aiPlusEnabled && !userId.equals("anonymous")) {
            log.info("AI+ 기능 활성화. 사용자(ID: {})의 개인 AI 키로 서머리를 생성합니다.", userId);
            try {
                Map<String, Object> personalizedResult = aiPersonalizationService.personalize(userId, executionContext);
                log.info("AI+ 서머리 생성 완료");
                return personalizedResult;
            } catch (Exception e) {
                log.warn("AI+ 서머리 생성 실패. 원본 데이터를 반환합니다. 오류: {}", e.getMessage());
            }
        }
        
        // AI+ 기능이 꺼진 경우, 모든 사용자에게 일관된 형태로 반환 ({"data": executionContext})
        Map<String, Object> result = new HashMap<>();
        result.put("data", executionContext);
        return result;
    }
}