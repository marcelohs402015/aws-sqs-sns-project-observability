package com.aws.sqs.service;

import com.aws.sqs.model.dto.QueueCreateRequest;
import com.aws.sqs.model.dto.QueueResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueManagementService {

    private final SqsAsyncClient sqsAsyncClient;

    public CompletableFuture<QueueResponse> createQueue(QueueCreateRequest request) {
        log.info("Creating queue: {}", request.queueName());

        var createQueueRequestBuilder = CreateQueueRequest.builder()
                .queueName(request.queueName());

        Map<QueueAttributeName, String> attributes = new HashMap<>();
        attributes.put(QueueAttributeName.VISIBILITY_TIMEOUT, String.valueOf(request.visibilityTimeoutSeconds()));
        attributes.put(QueueAttributeName.MESSAGE_RETENTION_PERIOD, String.valueOf(request.messageRetentionPeriodSeconds()));

        if (request.enableDlq()) {
            var dlqResponse = createDlq(request.queueName()).join();
            var redrivePolicy = String.format(
                    "{\"deadLetterTargetArn\":\"%s\",\"maxReceiveCount\":%d}",
                    dlqResponse.attributes().get(QueueAttributeName.QUEUE_ARN),
                    request.maxReceiveCount()
            );
            attributes.put(QueueAttributeName.REDRIVE_POLICY, redrivePolicy);
        }

        createQueueRequestBuilder.attributes(attributes);

        return sqsAsyncClient.createQueue(createQueueRequestBuilder.build())
                .thenCompose(createQueueResponse -> {
                    var queueUrl = createQueueResponse.queueUrl();
                    return getQueueAttributesByUrl(queueUrl)
                            .thenApply(attributesResponse -> {
                                var queueName = extractQueueNameFromUrl(queueUrl);
                                Map<String, String> attributeMap = attributesResponse.attributes().entrySet().stream()
                                        .collect(Collectors.toMap(
                                                entry -> entry.getKey().toString(),
                                                Map.Entry::getValue
                                        ));
                                return new QueueResponse(
                                        queueUrl,
                                        queueName,
                                        attributeMap,
                                        Instant.now()
                                );
                            });
                })
                .exceptionally(throwable -> {
                    log.error("Error creating queue: {}", request.queueName(), throwable);
                    throw new RuntimeException("Failed to create queue: " + request.queueName(), throwable);
                });
    }

    private CompletableFuture<GetQueueAttributesResponse> createDlq(String queueName) {
        var dlqName = queueName + "-dlq";
        log.info("Creating DLQ: {}", dlqName);

        return sqsAsyncClient.createQueue(CreateQueueRequest.builder()
                        .queueName(dlqName)
                        .build())
                .thenCompose(response -> sqsAsyncClient.getQueueAttributes(GetQueueAttributesRequest.builder()
                        .queueUrl(response.queueUrl())
                        .attributeNames(QueueAttributeName.QUEUE_ARN)
                        .build()));
    }

    private CompletableFuture<GetQueueAttributesResponse> getQueueAttributesByUrl(String queueUrl) {
        return sqsAsyncClient.getQueueAttributes(GetQueueAttributesRequest.builder()
                .queueUrl(queueUrl)
                .attributeNames(
                        QueueAttributeName.QUEUE_ARN,
                        QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES,
                        QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE,
                        QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED,
                        QueueAttributeName.CREATED_TIMESTAMP
                )
                .build());
    }

    public CompletableFuture<List<QueueResponse>> listQueues() {
        log.info("Listing all queues");

        return sqsAsyncClient.listQueues(ListQueuesRequest.builder().build())
                .thenCompose(listQueuesResponse -> {
                    var queueUrls = listQueuesResponse.queueUrls();

                    var futures = queueUrls.stream()
                            .map(queueUrl -> getQueueAttributesByUrl(queueUrl)
                                    .thenApply(response -> {
                                        var queueName = extractQueueNameFromUrl(queueUrl);
                                        Map<String, String> attributeMap = response.attributes().entrySet().stream()
                                                .collect(Collectors.toMap(
                                                        entry -> entry.getKey().toString(),
                                                        Map.Entry::getValue
                                                ));
                                        return new QueueResponse(
                                                queueUrl,
                                                queueName,
                                                attributeMap,
                                                Instant.now()
                                        );
                                    }))
                            .toList();

                    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                            .thenApply(v -> futures.stream()
                                    .map(CompletableFuture::join)
                                    .collect(Collectors.toList()));
                })
                .exceptionally(throwable -> {
                    log.error("Error listing queues", throwable);
                    throw new RuntimeException("Failed to list queues", throwable);
                });
    }

    public CompletableFuture<Map<String, String>> getQueueAttributes(String queueName) {
        log.info("Getting attributes for queue: {}", queueName);

        return getQueueUrl(queueName)
                .thenCompose(this::getQueueAttributesByUrl)
                .thenApply(response -> response.attributes().entrySet().stream()
                        .collect(Collectors.toMap(
                                entry -> entry.getKey().toString(),
                                Map.Entry::getValue
                        )))
                .exceptionally(throwable -> {
                    log.error("Error getting queue attributes: {}", queueName, throwable);
                    throw new RuntimeException("Failed to get queue attributes: " + queueName, throwable);
                });
    }

    public CompletableFuture<Void> deleteQueue(String queueName) {
        log.info("Deleting queue: {}", queueName);

        return getQueueUrl(queueName)
                .thenCompose(queueUrl -> sqsAsyncClient.deleteQueue(DeleteQueueRequest.builder()
                        .queueUrl(queueUrl)
                        .build())
                        .<Void>thenApply(response -> null))
                .exceptionally(throwable -> {
                    log.error("Error deleting queue: {}", queueName, throwable);
                    throw new RuntimeException("Failed to delete queue: " + queueName, throwable);
                });
    }

    private CompletableFuture<String> getQueueUrl(String queueName) {
        return sqsAsyncClient.getQueueUrl(GetQueueUrlRequest.builder()
                        .queueName(queueName)
                        .build())
                .thenApply(GetQueueUrlResponse::queueUrl);
    }

    private String extractQueueNameFromUrl(String queueUrl) {
        return queueUrl.substring(queueUrl.lastIndexOf('/') + 1);
    }
}
