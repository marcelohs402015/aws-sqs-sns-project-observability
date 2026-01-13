package com.aws.sqs.model.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Map;

public record MessageResponse(
        String messageId,
        String receiptHandle,
        JsonNode body,
        Map<String, String> attributes,
        Instant receivedAt
) {
}
