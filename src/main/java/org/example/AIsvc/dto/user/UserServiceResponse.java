package org.example.AIsvc.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserServiceResponse {
    private boolean success;
    private String message;
    private UserSecretResponse data;
    private String errorCode;
}