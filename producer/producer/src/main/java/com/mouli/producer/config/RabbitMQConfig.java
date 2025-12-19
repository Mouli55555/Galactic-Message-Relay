package com.mouli.producer.config;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class RabbitMQConfig {

//    public static final String COMMAND_EXCHANGE = "command.exchange";
//    public static final String COMMAND_QUEUE = "command.queue";
//    public static final String ROUTING_KEY = "command.key";
//
//    @Bean
//    public DirectExchange commandExchange() {
//        return new DirectExchange(COMMAND_EXCHANGE, true, false);
//    }

//    @Bean
//    public Queue commandQueue() {
//        return QueueBuilder.durable(COMMAND_QUEUE).build();
//    }
//
//    @Bean
//    public Binding commandBinding() {
//        return BindingBuilder
//                .bind(commandQueue())
//                .to(commandExchange())
//                .with(ROUTING_KEY);
//    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
