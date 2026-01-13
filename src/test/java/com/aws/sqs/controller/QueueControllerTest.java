package com.aws.sqs.controller;

import com.aws.sqs.model.dto.*;
import com.aws.sqs.service.MessageConsumerService;
import com.aws.sqs.service.MessagePublisherService;
import com.aws.sqs.service.QueueManagementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QueueController.class)
class QueueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private QueueManagementService queueManagementService;

    @MockBean
    private MessagePublisherService messagePublisherService;

    @MockBean
    private MessageConsumerService messageConsumerService;

    @Test
    void shouldCreateQueue() throws Exception {
        var request = new QueueCreateRequest("test-queue", false, 3, 30, 345600);
        var response = new QueueResponse(
                "http://localhost:4566/000000000000/test-queue",
                "test-queue",
                Map.of(),
                java.time.Instant.now()
        );

        when(queueManagementService.createQueue(any(QueueCreateRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        MvcResult mvcResult = mockMvc.perform(post("/api/queues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.queueName").value("test-queue"));
    }

    @Test
    void shouldPublishMessage() throws Exception {
        var messageRequest = new MessageRequest(Map.of("message", "test"));
        var messageId = "test-message-id";

        when(messagePublisherService.publishMessage(anyString(), any(MessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(messageId));

        MvcResult mvcResult = mockMvc.perform(post("/api/queues/test-queue/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(messageRequest)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.messageId").value(messageId));
    }

    @Test
    void shouldConsumeMessages() throws Exception {
        var consumeRequest = new ConsumeRequest(1, 0);
        var messageResponse = new MessageResponse(
                "test-message-id",
                "test-receipt-handle",
                objectMapper.readTree("{\"message\":\"test\"}"),
                Map.of(),
                java.time.Instant.now()
        );

        when(messageConsumerService.consumeMessages(anyString(), any(ConsumeRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(List.of(messageResponse)));

        MvcResult mvcResult = mockMvc.perform(post("/api/queues/test-queue/consume")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(consumeRequest)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].messageId").value("test-message-id"));
    }

    @Test
    void shouldListQueues() throws Exception {
        var queueResponse = new QueueResponse(
                "http://localhost:4566/000000000000/test-queue",
                "test-queue",
                Map.of(),
                java.time.Instant.now()
        );

        when(queueManagementService.listQueues())
                .thenReturn(CompletableFuture.completedFuture(List.of(queueResponse)));

        MvcResult mvcResult = mockMvc.perform(get("/api/queues"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].queueName").value("test-queue"));
    }
}
