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

    // ## @SneakyThrows: URI 생성 시 발생하는 체크 예외 처리를 Lombok에게 위임합니다.
    @SneakyThrows
    @Override
    public Object executeCustomApi(String customApiId, String query) {
        log.info("커스텀 API 실행 시작. API ID: {}, 쿼리: {}", customApiId, query);

        // ### 1. `커스텀API 서비스`를 호출하여 API 실행 계획('레시피')을 가져옵니다.
        CustomApiResponseDto recipe = customApiClient.getApiDetails(customApiId);

        // ## [6일차 추가] 2. 레시피에 있는 모든 API ID들을 추출하여 리스트로 만듭니다.
        List<String> apiIdsToResolve = recipe.getExternalApiInfoList().stream()
                .map(ExternalApiInfoDto::getApiId)
                .collect(Collectors.toList());

        // ## [6일차 추가] 3. `API관리 서비스`에 API ID 리스트를 보내 실제 URL이 담긴 Map을 받아옵니다.
        ApiUrlResponse apiUrlResponse = apiManagementClient.getApiUrls(new ApiUrlRequest(apiIdsToResolve));
        Map<String, String> apiUrlMap = apiUrlResponse.getApis().stream()
                .collect(Collectors.toMap(
                        ApiUrlResponse.ApiUrlDetail::getApiId,
                        ApiUrlResponse.ApiUrlDetail::getApiUrl
                ));
        log.info("{}개의 API URL 조회를 완료했습니다.", apiUrlMap.size());

        // ### 2. 모든 API 호출 결과를 저장하고, 다음 API의 입력으로 사용될 데이터 저장소(컨텍스트)를 생성합니다.
        Map<String, Object> executionContext = new HashMap<>();

        // ### 최초 사용자 쿼리를 컨텍스트에 추가합니다. (예: "location=서울" -> key: "location", value: "서울")
        Map<String, String> initialParams = UriComponentsBuilder.newInstance().query(query).build().getQueryParams().toSingleValueMap();
        executionContext.put("INITIAL_REQUEST", initialParams);

        // ### 3. 레시피에 적힌 순서대로 외부 API를 하나씩 호출합니다.
        for (ExternalApiInfoDto apiInfo : recipe.getExternalApiInfoList()) {
            log.info("단계 실행: {}", apiInfo.getApiName());

            // ### 3-1. 이번 단계 API 호출에 필요한 입력 파라미터를 컨텍스트에서 찾아 준비합니다.
            Map<String, Object> requestBody = new HashMap<>();
            for (ApiParameterDto param : apiInfo.getParameters()) {
                // ### 파라미터가 'INPUT' 타입일 경우에만 처리합니다.
                if ("INPUT".equals(param.getParamType())) {
                    // ### TODO: Day 7에서 parameter_mapping 로직을 여기에 구현합니다.
                    // ### 오늘은 간단히 최초 요청에서 값을 찾는다고 가정합니다.
                    if (initialParams.containsKey(param.getParamName())) {
                        requestBody.put(param.getParamName(), initialParams.get(param.getParamName()));
                    }
                }
            }

            // ## [6일차 수정] 3-2. apiInfo에서 직접 URL을 가져오는 대신, 조회해온 apiUrlMap에서 ID에 맞는 URL을 가져옵니다.
            String targetUrl = apiUrlMap.get(apiInfo.getApiId());
            if (targetUrl == null) {
                log.error("{}에 해당하는 URL을 찾을 수 없습니다. 오케스트레이션을 중단합니다.", apiInfo.getApiId());
                throw new IllegalStateException("URL for API ID " + apiInfo.getApiId() + " not found.");
            }
            URI targetUri = new URI(targetUrl);
            Map<String, Object> apiResult = genericApiClient.executePost(targetUri, requestBody);

            // ### 3-3. 호출 결과를 컨텍스트에 저장하여 다음 단계에서 사용할 수 있도록 합니다.
            executionContext.put(apiInfo.getApiId(), apiResult);
            log.info("단계 완료: {}. 결과: {}", apiInfo.getApiName(), apiResult);
        }

        // ### 4. 모든 단계가 끝난 후, 컨텍스트에 쌓인 결과들을 최종 결과로 반환합니다.
        log.info("커스텀 API 실행 완료. API ID: {}", customApiId);
        return executionContext;
    }
}