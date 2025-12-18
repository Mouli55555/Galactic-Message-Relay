package com.mouli.consumer.config;

import org.springframework.amqp.core.*;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String COMMAND_QUEUE = "command.queue";
    public static final String DLQ_QUEUE = "command.dlq";

    @Bean
    public Jackson2JsonMessageConverter jacksonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);

        DefaultJackson2JavaTypeMapper classMapper = new DefaultJackson2JavaTypeMapper();
        classMapper.setTrustedPackages("com.mouli.consumer.dto");
        classMapper.setTypePrecedence(DefaultJackson2JavaTypeMapper.TypePrecedence.INFERRED);

        converter.setJavaTypeMapper(classMapper); // for Jackson2JsonMessageConverter use setJavaTypeMapper
        return converter;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter converter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setDefaultRequeueRejected(false);

        // make prefetch explicit (matches your application.yml intent)
        factory.setPrefetchCount(1);

        return factory;
    }
    @Bean
    public Queue dlqQueue() {
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    @Bean
    public Queue commandQueue() {
        Map<String, Object> args = new HashMap<>();
        // when we explicitly route to the DLQ queue we can publish there,
        // but also useful to have a dead-letter routing if you ever nack with requeue=false
        args.put("x-dead-letter-exchange", ""); // default exchange
        args.put("x-dead-letter-routing-key", DLQ_QUEUE);
        return QueueBuilder.durable(COMMAND_QUEUE).withArguments(args).build();
    }
}
