package com.aws.sqs.controller;

import com.aws.sqs.model.dto.*;
import com.aws.sqs.service.MessageConsumerService;
import com.aws.sqs.service.MessagePublisherService;
import com.aws.sqs.service.QueueManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/queues")
@RequiredArgsConstructor
public class QueueController {

    private final QueueManagementService queueManagementService;
    private final MessagePublisherService messagePublisherService;
    private final MessageConsumerService messageConsumerService;

    @PostMapping
    public CompletableFuture<ResponseEntity<QueueResponse>> createQueue(
            @Valid @RequestBody QueueCreateRequest request) {
        log.info("Creating queue: {}", request.queueName());

        return queueManagementService.createQueue(request)
                .thenApply(queueResponse -> ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(queueResponse))
                .exceptionally(throwable -> {
                    log.error("Error creating queue", throwable);
                    return ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .build();
                });
    }

    @GetMapping
    public CompletableFuture<ResponseEntity<List<QueueResponse>>> listQueues() {
        log.info("Listing all queues");

        return queueManagementService.listQueues()
                .thenApply(ResponseEntity::ok)
                .exceptionally(throwable -> {
                    log.error("Error listing queues", throwable);
                    return ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .build();
                });
    }

    @GetMapping("/{queueName}/attributes")
    public CompletableFuture<ResponseEntity<Map<String, String>>> getQueueAttributes(
            @PathVariable String queueName) {
        log.info("Getting attributes for queue: {}", queueName);

        return queueManagementService.getQueueAttributes(queueName)
                .thenApply(ResponseEntity::ok)
                .exceptionally(throwable -> {
                    log.error("Error getting queue attributes", throwable);
                    return ResponseEntity
                            .status(HttpStatus.NOT_FOUND)
                            .build();
                });
    }

    @DeleteMapping("/{queueName}")
    public CompletableFuture<ResponseEntity<Void>> deleteQueue(
            @PathVariable String queueName) {
        log.info("Deleting queue: {}", queueName);

        return queueManagementService.deleteQueue(queueName)
                .<ResponseEntity<Void>>thenApply(v -> ResponseEntity.noContent().build())
                .exceptionally(throwable -> {
                    log.error("Error deleting queue", throwable);
                    return ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .<Void>build();
                });
    }

    @PostMapping("/{queueName}/messages")
    public CompletableFuture<ResponseEntity<Map<String, String>>> publishMessage(
            @PathVariable String queueName,
            @Valid @RequestBody MessageRequest messageRequest) {
        log.info("Publishing message to queue: {}", queueName);

        return messagePublisherService.publishMessage(queueName, messageRequest)
                .thenApply(messageId -> ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(Map.of("messageId", messageId)))
                .exceptionally(throwable -> {
                    log.error("Error publishing message", throwable);
                    return ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .build();
                });
    }

    @PostMapping("/{queueName}/consume")
    public CompletableFuture<ResponseEntity<List<MessageResponse>>> consumeMessages(
            @PathVariable String queueName,
            @Valid @RequestBody(required = false) ConsumeRequest consumeRequest) {
        log.info("Consuming messages from queue: {}", queueName);

        if (consumeRequest == null) {
            consumeRequest = new ConsumeRequest(null, null);
        }

        return messageConsumerService.consumeMessages(queueName, consumeRequest)
                .thenApply(ResponseEntity::ok)
                .exceptionally(throwable -> {
                    log.error("Error consuming messages", throwable);
                    return ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .build();
                });
    }

    @DeleteMapping("/{queueName}/messages/{receiptHandle}")
    public CompletableFuture<ResponseEntity<Void>> deleteMessage(
            @PathVariable String queueName,
            @PathVariable String receiptHandle) {
        log.info("Deleting message from queue: {}", queueName);

        return messageConsumerService.deleteMessage(queueName, receiptHandle)
                .<ResponseEntity<Void>>thenApply(v -> ResponseEntity.noContent().build())
                .exceptionally(throwable -> {
                    log.error("Error deleting message", throwable);
                    return ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .<Void>build();
                });
    }
}
