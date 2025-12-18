package com.mouli.consumer.service;

import com.mouli.consumer.dto.CommandMessage;
import com.mouli.consumer.exception.SimulatedProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class ProcessingService {

    private static final Logger log =
            LoggerFactory.getLogger(ProcessingService.class);

    private final Random random = new Random();

    public void process(CommandMessage message) {

        // 30% intentional failure
        if (random.nextInt(100) < 30) {
            log.warn("[SIMULATED_FAILURE] messageId={}",
                    message.getMessageId());
            throw new SimulatedProcessingException(
                    message.getMessageId());
        }

        log.info("[PROCESSING] messageId={} payload={}",
                message.getMessageId(),
                message.getPayload());
    }
}
