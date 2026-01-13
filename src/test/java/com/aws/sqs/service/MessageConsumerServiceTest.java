package com.aws.sqs.service;

import com.aws.sqs.model.dto.ConsumeRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageConsumerServiceTest {

    @Mock
    private SqsAsyncClient sqsAsyncClient;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MessageConsumerService messageConsumerService;

    private ConsumeRequest consumeRequest;

    @BeforeEach
    void setUp() {
        consumeRequest = new ConsumeRequest(1, 0);
    }

    @Test
    void shouldConsumeMessagesSuccessfully() throws Exception {
        var getQueueUrlResponse = GetQueueUrlResponse.builder()
                .queueUrl("http://localhost:4566/000000000000/test-queue")
                .build();

        var message = Message.builder()
                .messageId("test-message-id")
                .receiptHandle("test-receipt-handle")
                .body("{\"message\":\"test\"}")
                .build();

        var receiveMessageResponse = ReceiveMessageResponse.builder()
                .messages(List.of(message))
                .build();

        when(sqsAsyncClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(getQueueUrlResponse));

        when(sqsAsyncClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(receiveMessageResponse));

        when(objectMapper.readTree(anyString()))
                .thenReturn(new com.fasterxml.jackson.databind.ObjectMapper().readTree("{\"message\":\"test\"}"));

        var result = messageConsumerService.consumeMessages("test-queue", consumeRequest).join();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(sqsAsyncClient, times(1)).receiveMessage(any(ReceiveMessageRequest.class));
    }
}
