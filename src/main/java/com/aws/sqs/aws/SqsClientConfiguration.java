package com.aws.sqs.aws;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(AwsProperties.class)
public class SqsClientConfiguration {

    @Bean
    public SqsAsyncClient sqsAsyncClient(AwsProperties awsProperties) {
        var builder = SqsAsyncClient.builder()
                .region(Region.of(awsProperties.region()));

        if (awsProperties.endpointUrl() != null && !awsProperties.endpointUrl().isEmpty()) {
            builder.endpointOverride(URI.create(awsProperties.endpointUrl()));
        }

        if (awsProperties.accessKeyId() != null && !awsProperties.accessKeyId().isEmpty()
                && awsProperties.secretAccessKey() != null && !awsProperties.secretAccessKey().isEmpty()) {
            var credentials = AwsBasicCredentials.create(
                    awsProperties.accessKeyId(),
                    awsProperties.secretAccessKey()
            );
            builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }
}
