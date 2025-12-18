package com.mouli.producer.service;

import com.mouli.producer.dto.CommandMessage;
import jakarta.annotation.PostConstruct;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class MessagePublisherService {

    private final RabbitTemplate rabbitTemplate;

    public MessagePublisherService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostConstruct
    public void checkConverter() {
        System.out.println(rabbitTemplate.getMessageConverter().getClass());
    }

    public void publish(CommandMessage message) {
        rabbitTemplate.convertAndSend(
                "command.exchange",
                "command.key",
                message
        );
    }
}
