package com.mouli.consumer.listener;

import com.mouli.consumer.config.RabbitMQConfig;
import com.mouli.consumer.dto.CommandMessage;
import com.mouli.consumer.exception.SimulatedProcessingException;
import com.mouli.consumer.service.IdempotencyService;
import com.mouli.consumer.service.ProcessingService;

import com.rabbitmq.client.Channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CommandListener {
    private static final int MAX_RETRIES = 3;

    private final IdempotencyService idempotency;
    private final ProcessingService processor;
    private final RabbitTemplate rabbitTemplate;

    public CommandListener(IdempotencyService idempotency,
                           ProcessingService processor,
                           RabbitTemplate rabbitTemplate) {
        this.idempotency = idempotency;
        this.processor = processor;
        this.rabbitTemplate = rabbitTemplate;
    }

    private static final Logger log =
            LoggerFactory.getLogger(CommandListener.class);

    @RabbitListener(queues = RabbitMQConfig.COMMAND_QUEUE,
            containerFactory = "rabbitListenerContainerFactory")
    public void consume(CommandMessage message,
                        Message amqpMessage,
                        Channel channel) throws Exception {

        long tag = amqpMessage.getMessageProperties().getDeliveryTag();
        String id = message.getMessageId();
        log.info("[RECEIVED] messageId={}", id);

        // Claim with a token. If we couldn't claim, check if already processed; otherwise ignore duplicate.
        String claimToken = idempotency.claimProcessing(id);
        if (claimToken == null) {
            if (idempotency.isProcessed(id)) {
                log.info("[DUPLICATE_ALREADY_PROCESSED] messageId={}", id);
                channel.basicAck(tag, false);
                return;
            } else {
                // Another consumer is processing or claim expired — avoid concurrent processing
                log.info("[DUPLICATE_IGNORED] messageId={}", id);
                channel.basicAck(tag, false);
                return;
            }
        }

        try {
            processor.process(message);
            // mark processed BEFORE ack (requirement)
            idempotency.markProcessed(id);
            channel.basicAck(tag, false);
            log.info("[PROCESSED_SUCCESSFULLY] messageId={}", id);

        } catch (SimulatedProcessingException ex) {
            MessageProperties props = amqpMessage.getMessageProperties();
            Map<String, Object> headers = props.getHeaders();
            Integer attempts = (headers != null && headers.get("x-retries") instanceof Integer)
                    ? (Integer) headers.get("x-retries") : 0;
            attempts = attempts + 1;

            if (attempts <= MAX_RETRIES) {
                log.warn("[RETRYING attempt={} messageId={}]", attempts, id);

                // release claim so a retried message can be claimed again
                idempotency.releaseClaim(id, claimToken);

                // build new properties preserving original headers/type info
                MessageProperties newProps = new MessageProperties();
                // copy headers safely
                if (props.getHeaders() != null) {
                    newProps.getHeaders().putAll(new HashMap<>(props.getHeaders()));
                }
                newProps.setContentType(props.getContentType());
                newProps.setHeader("x-retries", attempts);

                org.springframework.amqp.core.Message newMsg =
                        MessageBuilder.withBody(amqpMessage.getBody())
                                .andProperties(newProps)
                                .build();

                // republish to primary queue via default exchange (route by queue name)
                rabbitTemplate.send("", RabbitMQConfig.COMMAND_QUEUE, newMsg);

                // ack current so broker won't redeliver this instance
                channel.basicAck(tag, false);
            } else {
                log.error("[MOVED_TO_DLQ] messageId={} after {} attempts", id, attempts);

                // release claim before moving to DLQ
                idempotency.releaseClaim(id, claimToken);

                // send to DLQ, preserving headers and adding metadata
                MessageProperties newProps = new MessageProperties();
                if (props.getHeaders() != null) {
                    newProps.getHeaders().putAll(new HashMap<>(props.getHeaders()));
                }
                newProps.setContentType(props.getContentType());
                newProps.setHeader("x-retries", attempts);
                newProps.setHeader("x-error-reason", ex.getMessage());
                newProps.setHeader("x-original-queue", RabbitMQConfig.COMMAND_QUEUE);

                org.springframework.amqp.core.Message newMsg =
                        MessageBuilder.withBody(amqpMessage.getBody())
                                .andProperties(newProps)
                                .build();

                rabbitTemplate.send("", RabbitMQConfig.DLQ_QUEUE, newMsg);
                channel.basicAck(tag, false);
            }

        } catch (Exception ex) {
            log.error("[UNEXPECTED_FAILURE_MOVED_TO_DLQ] messageId={}", id, ex);

            // release claim before moving to DLQ
            idempotency.releaseClaim(id, claimToken);

            // move immediately to DLQ (preserve original message)
            MessageProperties props = amqpMessage.getMessageProperties();
            MessageProperties newProps = new MessageProperties();
            if (props.getHeaders() != null) {
                newProps.getHeaders().putAll(new HashMap<>(props.getHeaders()));
            }
            newProps.setContentType(props.getContentType());
            newProps.setHeader("x-error-reason", ex.getMessage());
            newProps.setHeader("x-original-queue", RabbitMQConfig.COMMAND_QUEUE);

            org.springframework.amqp.core.Message newMsg =
                    MessageBuilder.withBody(amqpMessage.getBody())
                            .andProperties(newProps)
                            .build();

            rabbitTemplate.send("", RabbitMQConfig.DLQ_QUEUE, newMsg);
            channel.basicAck(tag, false);
        }
    }
}
