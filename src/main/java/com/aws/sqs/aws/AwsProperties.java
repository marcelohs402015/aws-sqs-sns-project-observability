package com.aws.sqs.aws;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws")
public record AwsProperties(
        String region,
        String endpointUrl,
        String accessKeyId,
        String secretAccessKey
) {
}
