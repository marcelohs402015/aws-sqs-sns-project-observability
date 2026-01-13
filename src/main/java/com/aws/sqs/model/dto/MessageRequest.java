package com.aws.sqs.model.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import jakarta.validation.constraints.NotNull;

import java.util.HashMap;
import java.util.Map;

public record MessageRequest(
        @NotNull(message = "Body cannot be null")
        Map<String, Object> body
) {
    public MessageRequest {
        if (body == null) {
            body = new HashMap<>();
        }
    }

    @JsonAnyGetter
    public Map<String, Object> body() {
        return body;
    }
}
