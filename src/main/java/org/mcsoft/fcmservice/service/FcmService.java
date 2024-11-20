package org.mcsoft.fcmservice.service;

import org.mcsoft.fcmservice.model.FcmMessage;

import java.io.IOException;

public interface FcmService {

    String send(FcmMessage fcmMessage) throws IOException;
}
