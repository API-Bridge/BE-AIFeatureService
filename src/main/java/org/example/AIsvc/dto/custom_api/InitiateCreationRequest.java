package org.example.AIsvc.dto.custom_api;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class InitiateCreationRequest {

    // API 생성을 요청한 사용자의 고유 ID (Auth0 ID)
    private String userId;

    // 생성될 커스텀 API의 고유 ID (사용자가 지정했거나, 시스템이 생성)
    private String customApiId;

    // AI 분석에 사용된 사용자의 원본 자연어 쿼리
    private String originalQuery;

    // AI 서비스가 분석한 도메인 목록 (문자열 리스트)
    private List<String> domains;

    // AI 서비스가 분석한 키워드 목록 (문자열 리스트)
    private List<String> keywords;
}