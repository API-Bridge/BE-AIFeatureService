package org.example.AIsvc.service;
import org.example.AIsvc.dto.request.AnalyzeQueryRequest;
import org.example.AIsvc.dto.response.AnalyzeQueryResponse;

public interface AIService {
    AnalyzeQueryResponse analyzeAndInitiateCreation(AnalyzeQueryRequest request);
}