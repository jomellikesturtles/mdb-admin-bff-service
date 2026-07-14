package com.mdb.adminbff.service;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class ConsumerService {

    private final Logger LOGGER = LogManager.getLogger(ConsumerService.class);

//    @KafkaListener(topics = "user.account.created", groupId = "user.account.created-0")
//    public void listenAccountCreated(String message) {
//        System.out.println("MESSAGE");
//        LOGGER.info("Kafka topic user.account.created message: {}", message);
//    }
//    @KafkaListener(topics = "crawl.failed", groupId = "crawl.failed-0")
//    public void listenCrawlFailed(String message) {
//        System.out.println("MESSAGE");
//        LOGGER.info("Kafka topic crawl-failed message: {}", message);
//    }
//
//    @KafkaListener(topics = "new-version", groupId = "new-version-0")
//    public void listenNewVersion(String message) {
//        LOGGER.info("Kafka topic new-version message: {}", message);
//    }

//    @KafkaListener(topics = "orders-topic", groupId = "group-id")
//    public void consume(@Payload OrderRequest order) {
//        System.out.println("Parsed Order: " + order.getOrderId());
//    }


//    @KafkaListener(topics = "advanced-topic")
//    public void handle(ConsumerRecord<String, String> record) {
//        String payload = record.value();
//        long timestamp = record.timestamp();
//        LOGGER.info("Kafka topic advanced-topic message: {}", payload);
//        // Manual parsing logic here
//    }

}
