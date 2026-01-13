package com.aws.sqs.service;

import com.aws.sqs.model.dto.MessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessagePublisherService {

    private final SqsAsyncClient sqsAsyncClient;
    private final ObjectMapper objectMapper;

    public CompletableFuture<String> publishMessage(String queueName, MessageRequest messageRequest) {
        log.info("Publishing message to queue: {}", queueName);

        return getQueueUrl(queueName)
                .thenCompose(queueUrl -> {
                    try {
                        var messageBody = objectMapper.writeValueAsString(messageRequest.body());
                        var sendMessageRequest = SendMessageRequest.builder()
                                .queueUrl(queueUrl)
                                .messageBody(messageBody)
                                .build();

                        return sqsAsyncClient.sendMessage(sendMessageRequest);
                    } catch (Exception e) {
                        log.error("Error serializing message body", e);
                        return CompletableFuture.failedFuture(e);
                    }
                })
                .thenApply(SendMessageResponse::messageId)
                .exceptionally(throwable -> {
                    log.error("Error publishing message to queue: {}", queueName, throwable);
                    throw new RuntimeException("Failed to publish message to queue: " + queueName, throwable);
                });
    }

    private CompletableFuture<String> getQueueUrl(String queueName) {
        return sqsAsyncClient.getQueueUrl(GetQueueUrlRequest.builder()
                        .queueName(queueName)
                        .build())
                .thenApply(GetQueueUrlResponse::queueUrl);
    }
}
