package org.example.AIsvc.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URI;
import java.util.Map;

@FeignClient(name = "generic-api-client")
public interface GenericApiClient {

    @PostMapping
    Map<String, Object> executePost(
            URI baseUrl,
            @RequestBody Map<String, Object> body
    );

    @GetMapping
    Map<String, Object> executeGet(
            URI baseUrl,
            @RequestParam Map<String, Object> queryParams
    );

    // TODO: 향후 PUT, DELETE 등 다른 HTTP 메소드를 위한 메소드도 필요에 따라 추가할 수 있음
}