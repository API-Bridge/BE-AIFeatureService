package org.example.AIsvc.service;

import java.util.List;
import java.util.Map;

public interface AIUsageService {
    /**
     * 외부 API ID 목록을 받아, 각 ID에 해당하는 실제 호출 URL로 변환하는 기능을 명세합니다.
     * @param apiIds 변환할 외부 API ID 목록
     * @return key: apiId, value: apiUrl인 Map 객체
     */
    Map<String, String> resolveApiUrls(List<String> apiIds);
}