package com.aws.sqs.metrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class QueueMetricsAspect {

    private final QueueMetrics queueMetrics;

    @Around("execution(* com.aws.sqs.service.QueueManagementService.createQueue(..))")
    public Object measureQueueCreation(ProceedingJoinPoint joinPoint) throws Throwable {
        var sample = queueMetrics.startProcessingTimer();
        String queueName = extractQueueName(joinPoint.getArgs()[0]);

        try {
            var result = joinPoint.proceed();
            if (result instanceof java.util.concurrent.CompletableFuture) {
                ((java.util.concurrent.CompletableFuture<?>) result).thenAccept(response -> {
                    queueMetrics.incrementQueueCreated(queueName);
                    queueMetrics.recordProcessingDuration(sample, queueName, "create");
                });
            }
            return result;
        } catch (Exception e) {
            queueMetrics.recordProcessingDuration(sample, queueName, "create");
            throw e;
        }
    }

    @Around("execution(* com.aws.sqs.service.MessagePublisherService.publishMessage(..))")
    public Object measureMessagePublish(ProceedingJoinPoint joinPoint) throws Throwable {
        var sample = queueMetrics.startProcessingTimer();
        String queueName = (String) joinPoint.getArgs()[0];
        final String statusSuccess = "success";

        try {
            var result = joinPoint.proceed();
            if (result instanceof java.util.concurrent.CompletableFuture) {
                ((java.util.concurrent.CompletableFuture<?>) result)
                        .thenAccept(response -> {
                            queueMetrics.incrementMessagesSent(queueName, statusSuccess);
                            queueMetrics.recordProcessingDuration(sample, queueName, "publish");
                        })
                        .exceptionally(throwable -> {
                            queueMetrics.incrementMessagesSent(queueName, "error");
                            queueMetrics.recordProcessingDuration(sample, queueName, "publish");
                            return null;
                        });
            }
            return result;
        } catch (Exception e) {
            queueMetrics.incrementMessagesSent(queueName, "error");
            queueMetrics.recordProcessingDuration(sample, queueName, "publish");
            throw e;
        }
    }

    @Around("execution(* com.aws.sqs.service.MessageConsumerService.consumeMessages(..))")
    public Object measureMessageConsume(ProceedingJoinPoint joinPoint) throws Throwable {
        var sample = queueMetrics.startProcessingTimer();
        String queueName = (String) joinPoint.getArgs()[0];
        final String statusSuccess = "success";

        try {
            var result = joinPoint.proceed();
            if (result instanceof java.util.concurrent.CompletableFuture) {
                ((java.util.concurrent.CompletableFuture<?>) result)
                        .thenAccept(response -> {
                            if (response instanceof java.util.List) {
                                var count = ((java.util.List<?>) response).size();
                                queueMetrics.incrementMessagesReceived(queueName, statusSuccess);
                                if (count > 0) {
                                    queueMetrics.setQueueSize(queueName, count);
                                }
                            }
                            queueMetrics.recordProcessingDuration(sample, queueName, "consume");
                        })
                        .exceptionally(throwable -> {
                            queueMetrics.incrementMessagesReceived(queueName, "error");
                            queueMetrics.recordProcessingDuration(sample, queueName, "consume");
                            return null;
                        });
            }
            return result;
        } catch (Exception e) {
            queueMetrics.incrementMessagesReceived(queueName, "error");
            queueMetrics.recordProcessingDuration(sample, queueName, "consume");
            throw e;
        }
    }

    private String extractQueueName(Object arg) {
        if (arg instanceof com.aws.sqs.model.dto.QueueCreateRequest request) {
            return request.queueName();
        }
        return "unknown";
    }
}
