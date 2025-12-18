package com.mouli.consumer.exception;

public class SimulatedProcessingException extends RuntimeException {

    public SimulatedProcessingException(String messageId) {
        super("Simulated processing failure for messageId=" + messageId);
    }
}
