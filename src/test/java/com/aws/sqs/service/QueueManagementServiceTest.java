package com.aws.sqs.service;

import com.aws.sqs.model.dto.QueueCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueueManagementServiceTest {

    @Mock
    private SqsAsyncClient sqsAsyncClient;

    @InjectMocks
    private QueueManagementService queueManagementService;

    private QueueCreateRequest request;

    @BeforeEach
    void setUp() {
        request = new QueueCreateRequest(
                "test-queue",
                false,
                3,
                30,
                345600
        );
    }

    @Test
    void shouldCreateQueueSuccessfully() {
        var createQueueResponse = CreateQueueResponse.builder()
                .queueUrl("http://localhost:4566/000000000000/test-queue")
                .build();

        var getQueueAttributesResponse = GetQueueAttributesResponse.builder()
                .attributes(java.util.Map.of(
                        QueueAttributeName.QUEUE_ARN, "arn:aws:sqs:us-east-1:000000000000:test-queue"
                ))
                .build();

        when(sqsAsyncClient.createQueue(any(CreateQueueRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(createQueueResponse));

        when(sqsAsyncClient.getQueueAttributes(any(GetQueueAttributesRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(getQueueAttributesResponse));

        var result = queueManagementService.createQueue(request).join();

        assertNotNull(result);
        assertEquals("test-queue", result.queueName());
        verify(sqsAsyncClient, times(1)).createQueue(any(CreateQueueRequest.class));
    }

    @Test
    void shouldHandleCreateQueueError() {
        when(sqsAsyncClient.createQueue(any(CreateQueueRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("AWS Error")));

        assertThrows(RuntimeException.class, () -> {
            queueManagementService.createQueue(request).join();
        });
    }
}
