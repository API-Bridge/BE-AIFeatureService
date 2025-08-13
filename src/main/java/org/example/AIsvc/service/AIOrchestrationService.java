package org.example.AIsvc.service;

// 생성된 커스텀 API의 실행을 담당하는 서비스
public interface AIOrchestrationService {
    /**
     * 특정 커스텀 API를 주어진 쿼리로 실행
     * @param customApiId 실행할 커스텀 API의 고유 ID
     * @param query 사용자가 입력한 쿼리 (파라미터 포함)
     * @return 오케스트레이션 결과 (최종 조합된 데이터)
     */
    Object executeCustomApi(String customApiId, String query);
}