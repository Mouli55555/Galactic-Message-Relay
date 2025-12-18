package com.mouli.consumer.controller;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static com.mouli.consumer.config.RabbitMQConfig.*;

@RestController
public class StatusController {

    private final RabbitTemplate rabbitTemplate;

    public StatusController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        try {
            int primary = rabbitTemplate.execute(channel ->
                    channel.queueDeclarePassive(COMMAND_QUEUE).getMessageCount());
            int poison = rabbitTemplate.execute(channel ->
                    channel.queueDeclarePassive(DLQ_QUEUE).getMessageCount());
            return Map.of("primaryQueue", primary, "poisonQueue", poison);
        } catch (Exception ex) {
            return Map.of("primaryQueue", 0, "poisonQueue", 0, "error", ex.getMessage());
        }
    }

}
