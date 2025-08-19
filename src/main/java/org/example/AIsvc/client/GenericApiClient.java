package org.example.AIsvc.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.net.URI;
import java.util.Map;

@FeignClient(name = "generic-api-client")
@org.springframework.context.annotation.Profile("!demo")
public interface GenericApiClient {

    @PostMapping
    Map<String, Object> executePost(
            URI baseUrl,
            @RequestBody Map<String, Object> body
    );

    // TODO: 향후 GET, PUT, DELETE 등 다른 HTTP 메소드를 위한 메소드도 필요에 따라 추가할 수 있음
}