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

        // atomic "claim" to avoid race (see IdempotencyService improvement below)
        if (!idempotency.claimProcessing(id)) {
            log.info("[DUPLICATE_IGNORED] messageId={}", id);
            channel.basicAck(tag, false);
            return;
        }

        try {
            processor.process(message);
            idempotency.markProcessed(id); // final mark
            channel.basicAck(tag, false);
            log.info("[PROCESSED_SUCCESSFULLY] messageId={}", id);

        } catch (SimulatedProcessingException ex) {
            // read attempts header and republish or move to DLQ
            MessageProperties props = amqpMessage.getMessageProperties();
            Map<String, Object> headers = props.getHeaders();
            Integer attempts = (headers != null && headers.get("x-retries") instanceof Integer)
                    ? (Integer) headers.get("x-retries") : 0;
            attempts = attempts + 1;

            if (attempts <= MAX_RETRIES) {
                log.warn("[RETRYING attempt={} messageId={}]", attempts, id);

                MessageProperties newProps = new MessageProperties();
                newProps.setContentType(props.getContentType());
                newProps.setHeader("x-retries", attempts);
                // copy other important headers if needed

                org.springframework.amqp.core.Message newMsg =
                        MessageBuilder.withBody(amqpMessage.getBody())
                                .andProperties(newProps)
                                .build();

                // republish to primary queue (immediate retry)
                rabbitTemplate.send(RabbitMQConfig.COMMAND_QUEUE, newMsg);

                // ack the current delivery so it won't be redelivered by the broker
                channel.basicAck(tag, false);
            } else {
                log.error("[MOVED_TO_DLQ] messageId={} after {} attempts", id, attempts);
                // send to DLQ
                MessageProperties newProps = new MessageProperties();
                newProps.setContentType(props.getContentType());
                newProps.setHeader("x-retries", attempts);

                org.springframework.amqp.core.Message newMsg =
                        MessageBuilder.withBody(amqpMessage.getBody())
                                .andProperties(newProps)
                                .build();

                rabbitTemplate.send(RabbitMQConfig.DLQ_QUEUE, newMsg);
                channel.basicAck(tag, false);
            }

        } catch (Exception ex) {
            log.error("[UNEXPECTED_FAILURE_MOVED_TO_DLQ] messageId={}", id, ex);
            // move immediately to DLQ
            rabbitTemplate.send(RabbitMQConfig.DLQ_QUEUE, amqpMessage);
            channel.basicAck(tag, false);
        }
    }
}
