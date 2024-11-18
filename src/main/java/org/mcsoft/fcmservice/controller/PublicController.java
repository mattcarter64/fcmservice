package org.mcsoft.fcmservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.mcsoft.fcmservice.model.FcmMessage;
import org.mcsoft.fcmservice.service.FcmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Slf4j
@RestController
public class PublicController {

    private final FcmService fcmService;

    @Autowired
    public PublicController(FcmService fcmService) {
        this.fcmService = fcmService;
    }

    @PostMapping(path = "/api/{version}/fcmService/send")
    public void sendFcmMessage(@RequestBody FcmMessage fcmMessage) throws IOException {
        fcmService.send(fcmMessage);
    }
}
