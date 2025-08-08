package org.example.AIsvc.service;
import lombok.RequiredArgsConstructor;
import org.example.AIsvc.dto.request.AnalyzeQueryRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {
    @Override
    public void analyzeAndInitiateCreation(AnalyzeQueryRequest request) {
        // ### 아직 실제 로직은 없음.
    }
}