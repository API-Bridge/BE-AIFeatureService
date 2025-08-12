package org.example.AIsvc.dto.api_management;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ApiUrlRequest {
    private List<String> apiIds;
}