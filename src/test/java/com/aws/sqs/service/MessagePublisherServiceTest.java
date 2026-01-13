package com.aws.sqs.service;

import com.aws.sqs.model.dto.MessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessagePublisherServiceTest {

    @Mock
    private SqsAsyncClient sqsAsyncClient;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MessagePublisherService messagePublisherService;

    private MessageRequest messageRequest;

    @BeforeEach
    void setUp() {
        Map<String, Object> body = new HashMap<>();
        body.put("message", "test");
        messageRequest = new MessageRequest(body);
    }

    @Test
    void shouldPublishMessageSuccessfully() throws Exception {
        var getQueueUrlResponse = GetQueueUrlResponse.builder()
                .queueUrl("http://localhost:4566/000000000000/test-queue")
                .build();

        var sendMessageResponse = SendMessageResponse.builder()
                .messageId("test-message-id")
                .build();

        when(sqsAsyncClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(getQueueUrlResponse));

        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"message\":\"test\"}");

        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(sendMessageResponse));

        var result = messagePublisherService.publishMessage("test-queue", messageRequest).join();

        assertNotNull(result);
        assertEquals("test-message-id", result);
        verify(sqsAsyncClient, times(1)).sendMessage(any(SendMessageRequest.class));
    }
}
