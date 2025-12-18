package com.mouli.consumer.config;

import com.mouli.consumer.exception.SimulatedProcessingException;
import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RabbitListenerErrorHandler extends ConditionalRejectingErrorHandler {

    private static final Logger log =
            LoggerFactory.getLogger(RabbitListenerErrorHandler.class);

    @Override
    public void handleError(Throwable t) {
        if (t instanceof ListenerExecutionFailedException lefe) {
            Throwable cause = lefe.getCause();

            if (cause instanceof SimulatedProcessingException) {
                log.info("[INTENTIONAL_FAILURE] {}", cause.getMessage());
                return; // suppress WARN
            }
        }
        log.error("[UNEXPECTED_RABBIT_ERROR]", t);
    }

}
