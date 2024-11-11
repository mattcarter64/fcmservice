package org.mcsoft.fcmservice.nimbusRestFactory;

import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

public class MediaOverrideInterceptor implements ClientHttpRequestInterceptor {
    private MediaType mediaType;

    public MediaOverrideInterceptor(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        ClientHttpResponse response = execution.execute(request, body);
        response.getHeaders().setContentType(this.mediaType);
        return response;
    }
}
