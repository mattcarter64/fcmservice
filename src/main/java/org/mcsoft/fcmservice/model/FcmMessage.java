package org.mcsoft.fcmservice.model;

import lombok.Data;

//{
//        "message": {
//        "topic": "news",
//        "notification": {
//        "title": "Breaking News",
//        "body": "New news story available."
//        },
//        "data": {
//        "story_id": "story_12345"
//        }
//        }
//}

@Data
public class FcmMessage {

    private Message message;

    public FcmMessage(String topic, String title, String body) {
        this.message = new Message(topic, new Notification(title, body));
    }
}
