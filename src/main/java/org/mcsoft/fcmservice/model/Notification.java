package org.mcsoft.fcmservice.model;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class Notification {
    private String title;
    private String body;

    public Notification(String title, String body) {
        this.title = title;
        this.body = body;
    }
}
