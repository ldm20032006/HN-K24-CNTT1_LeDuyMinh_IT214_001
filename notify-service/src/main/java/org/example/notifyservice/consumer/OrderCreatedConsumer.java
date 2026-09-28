package org.example.notifyservice.consumer;

import org.example.notifyservice.service.EmailService;

public class OrderCreatedConsumer {

    private final EmailService emailService;

    public OrderCreatedConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    public void consume(String email) {
        throw new UnsupportedOperationException();
    }
}
