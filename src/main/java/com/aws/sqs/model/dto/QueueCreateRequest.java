package com.aws.sqs.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;

public record QueueCreateRequest(
        @NotBlank(message = "Queue name is required")
        String queueName,
        
        Boolean enableDlq,
        
        @Min(value = 1, message = "Max receive count must be at least 1")
        Integer maxReceiveCount,
        
        Integer visibilityTimeoutSeconds,
        
        Integer messageRetentionPeriodSeconds
) {
    public QueueCreateRequest {
        if (enableDlq == null) {
            enableDlq = false;
        }
        if (maxReceiveCount == null) {
            maxReceiveCount = 3;
        }
        if (visibilityTimeoutSeconds == null) {
            visibilityTimeoutSeconds = 30;
        }
        if (messageRetentionPeriodSeconds == null) {
            messageRetentionPeriodSeconds = 345600;
        }
    }
}
