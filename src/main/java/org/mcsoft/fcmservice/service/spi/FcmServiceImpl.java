package org.mcsoft.fcmservice.service.spi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.mcsoft.fcmservice.model.FcmMessage;
import org.mcsoft.fcmservice.nimbusRestFactory.RestTemplateFactory;
import org.mcsoft.fcmservice.service.FcmService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Objects;

@Slf4j
@Service
public class FcmServiceImpl implements FcmService {

    private static final String FCM_TOPIC = "mcsoft";
    private static final String FCM_PROJECT_ID = "homeauto-60384";
    private static final String MESSAGING_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
    private static final String[] SCOPES = {MESSAGING_SCOPE};
    private static final String FMC_URL = "https://fcm.googleapis.com/v1/projects/" + FCM_PROJECT_ID + "/messages:send";
    private final ObjectMapper objectMapper;

    @Value("${google.fcm.key}")
    private String fcmApiKey;

    private RestTemplate restTemplate;
    private RestTemplateFactory restTemplateFactory;

    public FcmServiceImpl(RestTemplateFactory restTemplateFactory, ObjectMapper objectMapper) {
        this.restTemplateFactory = restTemplateFactory;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() throws IOException {
        restTemplate = restTemplateFactory.createRestTemplate(10000);

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(getResource(fcmApiKey))
                        .createScoped(Arrays.asList(SCOPES)))
                .setProjectId(FCM_PROJECT_ID)
                .build();

        FirebaseApp.initializeApp(options);
    }

    @Override
    public String send(FcmMessage fcmMessage) throws IOException {

        log.info("send: fcmmessage: {},", fcmMessage);

        Message message = Message.builder()

                .setTopic(FCM_TOPIC)
                .setNotification(Notification.builder()
                        .setTitle(fcmMessage.getNotification().getTitle())
                        .setBody(fcmMessage.getNotification().getBody())
                        .build())
                .putAllData(fcmMessage.getData())
                .build();

        log.info("send: fcmmessage: {}, message: {},", fcmMessage, message);

        try {
            String response = FirebaseMessaging.getInstance().send(message);

            log.info("response: {}", response);

            return response;
        } catch (FirebaseMessagingException e) {
            log.warn("Error sending notification", e);
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    private InputStream getResource(String resource) throws IOException {
        return Objects.requireNonNull(getClass().getResource(resource)).openStream();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private String getAccessToken() throws IOException {
        GoogleCredentials googleCredentials = GoogleCredentials.fromStream(getResource(fcmApiKey))
                .createScoped(Arrays.asList(SCOPES));
        googleCredentials.refresh();

        return googleCredentials.getAccessToken().getTokenValue();
    }

    private HttpHeaders setupPOSTHeaders() {
        HttpHeaders headers = new HttpHeaders();

        headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        return headers;
    }

    private UriComponentsBuilder getUriComponentsBuilder(String path) {
        return UriComponentsBuilder.fromUriString(FMC_URL + path);
    }

    private <T> ResponseEntity<T> sendGET(URI uri, HttpEntity<?> httpEntity, Class<T> responseType,
                                          int timeout) {

        log.info("Sending GET request to URI={}", uri.toString());

        return restTemplate.exchange(uri, HttpMethod.GET, httpEntity, responseType);
    }

    private <T> ResponseEntity<T> sendPOST(URI uri, HttpEntity<?> httpEntity, Class<T> responseType,
                                           int timeout) throws IOException {

        log.info("Sending POST request to URI={}", uri.toString());

        return restTemplate.exchange(uri, HttpMethod.POST, httpEntity, responseType);
    }

    private <T> ResponseEntity<T> sendPOST(URI uri, HttpEntity<?> httpEntity, ParameterizedTypeReference<T> responseType,
                                           int timeout) {

        log.info("Sending POST request to URI={}", uri.toString());

        return restTemplate.exchange(uri, HttpMethod.POST, httpEntity, responseType);
    }

}
