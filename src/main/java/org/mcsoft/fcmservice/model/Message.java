package org.mcsoft.fcmservice.model;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class Message {
    private String topic;
    private Notification notification;
    private Map<String, String> data = new HashMap<>();

    public Message(String topic, Notification notification) {
        this.topic = topic;
        this.notification = notification;
    }

    public Message(String topic) {
        this.topic = topic;
    }
}
