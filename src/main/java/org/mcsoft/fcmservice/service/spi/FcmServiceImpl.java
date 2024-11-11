package org.mcsoft.fcmservice.service.spi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.mcsoft.fcmservice.model.FcmMessage;
import org.mcsoft.fcmservice.nimbusRestFactory.NimbusRestTemplateFactory;
import org.mcsoft.fcmservice.service.FcmService;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Objects;

@Slf4j
@Service
public class FcmServiceImpl implements FcmService {

    private static final String FCM_PROJECT_ID = "homeauto-60384";
    private static final String MESSAGING_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
    private static final String[] SCOPES = {MESSAGING_SCOPE};
    private static final String FCM_CREDENTIALS = "homeauto-60384-24ba8e25748c.json";
    private static final String FMC_URL = "https://fcm.googleapis.com/v1/projects/" + FCM_PROJECT_ID + "/messages:send";
    private final ObjectMapper objectMapper;

    private RestTemplate restTemplate;
    private NimbusRestTemplateFactory nimbusRestTemplateFactory;

    public FcmServiceImpl(NimbusRestTemplateFactory nimbusRestTemplateFactory, ObjectMapper objectMapper) {
        this.nimbusRestTemplateFactory = nimbusRestTemplateFactory;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        restTemplate = nimbusRestTemplateFactory.createRestTemplate(10000);
    }

    @Override
    public void send(FcmMessage message) throws IOException {

        HttpHeaders headers = setupPOSTHeaders();

        headers.add("Authorization", "Bearer " + getAccessToken());

        String notification = objectMapper.writeValueAsString(message);

        log.info("send: message={}, headers={}, notification={}", message, headers, notification);

        // String json = "{ \"to\": \"/topics/news\", \"notification\": { \"title\":\"CONTACT\", \"body\":
        // \"Door opened\" }, }";

        URI uri = UriComponentsBuilder.newInstance().fromUriString(FMC_URL).build().toUri();

        InputStream content = new ByteArrayInputStream(notification.getBytes());

        try {
            ResponseEntity<String> responseEntity = sendPOST(uri,
                    new HttpEntity<>(notification, headers), String.class, 15000);

            if (responseEntity.getBody() != null) {
                log.info("response={}", responseEntity.getBody());
            }
        } catch (IOException e) {
            log.error("send: error sending request", e);
        }
    }

    private String getAccessToken() throws IOException {
        GoogleCredentials googleCredentials = GoogleCredentials.fromStream(getResource(FCM_CREDENTIALS))
                .createScoped(Arrays.asList(SCOPES));
        googleCredentials.refresh();
        return googleCredentials.getAccessToken().getTokenValue();
    }

    @SneakyThrows
    private InputStream getResource(String resource) throws IOException {
        return Objects.requireNonNull(getClass().getResource(resource)).openStream();
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
