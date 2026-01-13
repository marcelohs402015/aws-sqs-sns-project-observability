package com.aws.sqs.model.dto;

import java.time.Instant;
import java.util.Map;

public record QueueResponse(
        String queueUrl,
        String queueName,
        Map<String, String> attributes,
        Instant createdAt
) {
}
