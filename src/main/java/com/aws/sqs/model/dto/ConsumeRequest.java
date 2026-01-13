package com.aws.sqs.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ConsumeRequest(
        @Min(value = 1, message = "Max messages must be at least 1")
        @Max(value = 10, message = "Max messages cannot exceed 10")
        Integer maxMessages,
        
        @Min(value = 0, message = "Wait time cannot be negative")
        @Max(value = 20, message = "Wait time cannot exceed 20 seconds")
        Integer waitTimeSeconds
) {
    public ConsumeRequest {
        if (maxMessages == null) {
            maxMessages = 1;
        }
        if (waitTimeSeconds == null) {
            waitTimeSeconds = 0;
        }
    }
}
