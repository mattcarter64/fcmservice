package org.mcsoft.fcmservice.service;

import org.mcsoft.fcmservice.model.FcmMessage;

import java.io.IOException;

public interface FcmService {

    public void send(FcmMessage message) throws IOException;
}
