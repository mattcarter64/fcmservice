package org.mcsoft.fcmservice.model;

import lombok.Data;

@Data
public class Notification {
    private String title;
    private String body;
    // private int id;

    public Notification(String title, String body) {
        this.title = title;
        this.body = body;
        // this.id = id;
    }
}
