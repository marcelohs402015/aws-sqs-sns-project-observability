package com.aws.sqs.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class QueueMetrics {

    private final MeterRegistry meterRegistry;
    private final ConcurrentMap<String, Counter> messagesSentCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> messagesReceivedCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> queueCreatedCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Timer> processingTimers = new ConcurrentHashMap<>();

    public QueueMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void incrementMessagesSent(String queueName, String status) {
        var counter = messagesSentCounters.computeIfAbsent(
                queueName + "." + status,
                key -> Counter.builder("sqs.messages.sent.total")
                        .tag("queue_name", queueName)
                        .tag("status", status)
                        .register(meterRegistry)
        );
        counter.increment();
    }

    public void incrementMessagesReceived(String queueName, String status) {
        var counter = messagesReceivedCounters.computeIfAbsent(
                queueName + "." + status,
                key -> Counter.builder("sqs.messages.received.total")
                        .tag("queue_name", queueName)
                        .tag("status", status)
                        .register(meterRegistry)
        );
        counter.increment();
    }

    public void incrementQueueCreated(String queueName) {
        var counter = queueCreatedCounters.computeIfAbsent(
                queueName,
                key -> Counter.builder("sqs.queue.created.total")
                        .tag("queue_name", queueName)
                        .register(meterRegistry)
        );
        counter.increment();
    }

    public Timer.Sample startProcessingTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordProcessingDuration(Timer.Sample sample, String queueName, String operation) {
        var timer = processingTimers.computeIfAbsent(
                queueName + "." + operation,
                key -> Timer.builder("sqs.message.processing.duration")
                        .tag("queue_name", queueName)
                        .tag("operation", operation)
                        .register(meterRegistry)
        );
        sample.stop(timer);
    }

    public void setQueueSize(String queueName, long size) {
        Gauge.builder("sqs.queue.size", () -> size)
                .tag("queue_name", queueName)
                .register(meterRegistry);
    }
}
