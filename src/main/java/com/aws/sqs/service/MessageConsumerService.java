package com.aws.sqs.service;

import com.aws.sqs.model.dto.ConsumeRequest;
import com.aws.sqs.model.dto.MessageResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageConsumerService {

    private final SqsAsyncClient sqsAsyncClient;
    private final ObjectMapper objectMapper;

    public CompletableFuture<List<MessageResponse>> consumeMessages(String queueName, ConsumeRequest consumeRequest) {
        log.info("Consuming messages from queue: {} (maxMessages: {}, waitTime: {})",
                queueName, consumeRequest.maxMessages(), consumeRequest.waitTimeSeconds());

        return getQueueUrl(queueName)
                .thenCompose(queueUrl -> {
                    var receiveMessageRequest = ReceiveMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .maxNumberOfMessages(consumeRequest.maxMessages())
                            .waitTimeSeconds(consumeRequest.waitTimeSeconds())
                            .attributeNames(QueueAttributeName.ALL)
                            .build();

                    return sqsAsyncClient.receiveMessage(receiveMessageRequest);
                })
                .thenApply(receiveMessageResponse -> receiveMessageResponse.messages().stream()
                        .map(message -> {
                            try {
                                JsonNode body = objectMapper.readTree(message.body());
                                Map<String, String> attributes = message.attributes().entrySet().stream()
                                        .collect(Collectors.toMap(
                                                entry -> entry.getKey().toString(),
                                                Map.Entry::getValue
                                        ));

                                return new MessageResponse(
                                        message.messageId(),
                                        message.receiptHandle(),
                                        body,
                                        attributes,
                                        Instant.now()
                                );
                            } catch (Exception e) {
                                log.error("Error parsing message body", e);
                                throw new RuntimeException("Failed to parse message", e);
                            }
                        })
                        .collect(Collectors.toList()))
                .exceptionally(throwable -> {
                    log.error("Error consuming messages from queue: {}", queueName, throwable);
                    throw new RuntimeException("Failed to consume messages from queue: " + queueName, throwable);
                });
    }

    public CompletableFuture<Void> deleteMessage(String queueName, String receiptHandle) {
        log.info("Deleting message from queue: {}", queueName);

        return getQueueUrl(queueName)
                .thenCompose(queueUrl -> sqsAsyncClient.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(receiptHandle)
                        .build())
                        .<Void>thenApply(response -> null))
                .exceptionally(throwable -> {
                    log.error("Error deleting message from queue: {}", queueName, throwable);
                    throw new RuntimeException("Failed to delete message from queue: " + queueName, throwable);
                });
    }

    private CompletableFuture<String> getQueueUrl(String queueName) {
        return sqsAsyncClient.getQueueUrl(GetQueueUrlRequest.builder()
                        .queueName(queueName)
                        .build())
                .thenApply(GetQueueUrlResponse::queueUrl);
    }
}
