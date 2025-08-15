package org.example.AIsvc.service;

import java.util.Map;

// AI+ 개인화 로직을 전담하는 서비스의 인터페이스
public interface AIPersonalizationService {
    /**
     * 사용자의 구독 플랜에 따라 원본 데이터를 AI로 가공
     * @param userId 요청을 보낸 사용자의 ID
     * @param rawData 외부 API 호출을 통해 조합된 원본 데이터
     * @return AI에 의해 개인화된 데이터가 포함된 Map 객체
     */
    Map<String, Object> personalize(String userId, Object rawData);
}