package org.example.AIsvc.dto.api_management;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ApiUrlResponse {
    private List<ApiUrlDetail> apis;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiUrlDetail {
        // 외부 API의 고유 ID
        private String apiId;
        // 외부 API를 호출할 수 있는 실제 URL
        private String apiUrl;
    }
}