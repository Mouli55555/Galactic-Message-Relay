package com.mouli.producer.controller;

import com.mouli.producer.dto.CommandMessage;
import com.mouli.producer.dto.CommandRequest;
import com.mouli.producer.service.MessagePublisherService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/command")
public class CommandController {

    private final MessagePublisherService publisherService;

    public CommandController(MessagePublisherService publisherService) {
        this.publisherService = publisherService;
    }

    @PostMapping
    public ResponseEntity<?> sendCommand(@Valid @RequestBody CommandRequest request) {

        CommandMessage message =
                new CommandMessage(request.getMessageId(), request.getPayload());

        publisherService.publish(message);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(Map.of(
                        "status", "ACCEPTED",
                        "messageId", request.getMessageId()
                ));
    }
}
