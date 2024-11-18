package org.mcsoft.fcmservice.model;

import lombok.Data;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Data
@ToString
public class FcmMessage {
    private static AtomicInteger messageId = new AtomicInteger(1);

    private Notification notification;
    private Map<String, String> data = new HashMap<>();

    public FcmMessage() {
    }

    public FcmMessage(String title, String text) {
        this(null, new Notification(title, text));
    }

    public FcmMessage(String topic, Notification notification) {
        this.notification = notification;

        this.data.put("id", messageId.getAndIncrement() + "");
    }
}
