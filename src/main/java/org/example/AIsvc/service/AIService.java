package org.example.AIsvc.service;
import org.example.AIsvc.dto.request.AnalyzeQueryRequest;

public interface AIService {
    // Day 1에서는 반환 타입이 중요하지 않으므로 void로 우선 정의
    void analyzeAndInitiateCreation(AnalyzeQueryRequest request);
}