package org.example.AIsvc.dto.gemini;

import lombok.Getter;
import java.util.Collections;
import java.util.List;

// Gemini API에 보낼 요청의 구조를 정의하는 DTO.
@Getter
public class GeminiRequest {

    // AI에게 보낼 콘텐츠 목록
    private List<Content> contents;

    @Getter
    public static class Content {
        private List<Part> parts;
    }

    @Getter
    public static class Part {
        private String text; // 실제 텍스트 프롬프트 내용
    }

    public GeminiRequest(String prompt) {
        Part part = new Part();
        part.text = prompt;

        Content content = new Content();
        content.parts = Collections.singletonList(part);

        this.contents = Collections.singletonList(content);
    }
}