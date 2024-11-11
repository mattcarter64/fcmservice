package org.mcsoft.fcmservice.nimbusRestFactory;


import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.FileCopyUtils;
//import org.springframework.http.HttpRequest;
//import org.springframework.http.client.ClientHttpRequestExecution;
//import org.springframework.http.client.ClientHttpRequestInterceptor;
//import org.springframework.http.client.ClientHttpResponse;
//import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.util.Map;

public class LoggingRequestInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger restMessageLogger = LoggerFactory.getLogger(LoggingRequestInterceptor.class);
    private static final Logger restStatsLogger = LoggerFactory.getLogger("com.att.digitallife.hnm.common.utils.nimbusRestFactory.RestStatsLogger");

    private static final String MDC_KEY_API = "api";
    private static final String MDC_KEY_MSG_TYPE = "msgType";
    private static final String MDC_KEY_TIME_TAKEN = "timeTaken";
    private static final String MDC_KEY_HTTP_STATUS = "httpReturnStatusAsNum";

    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

        StringBuilder sb = new StringBuilder("Outgoing REST Request:\n");

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        String methodAndUri = "";

        Map<String, String> previousContext = MDC.getCopyOfContextMap();
        try {
            MDC.put(MDC_KEY_API, request.getURI().getScheme() + "://" + request.getURI().getHost() + request.getURI().getPath());

            methodAndUri = request.getMethod() + " " + request.getURI();
            sb.append(methodAndUri).append("\n");

            sb.append("\n");

            sb.append("Request Headers:\n");
            StringBuilder requestHeaders = new StringBuilder();
            request.getHeaders().forEach((key, value) -> requestHeaders.append(key).append(": ").append(value).append("\n"));
            sb.append(requestHeaders);

            sb.append("\n");

            sb.append("Request Body:\n");
            sb.append(new String(body));

            MDC.put(MDC_KEY_MSG_TYPE, "REST-OB-REQ");
            restMessageLogger.info(sb.toString());

            ClientHttpResponse response = execution.execute(request, body);
            long responseTime = stopWatch.getTime();

            MDC.put(MDC_KEY_HTTP_STATUS, String.valueOf(response.getStatusCode().value()));

            sb = new StringBuilder("Outgoing REST Response:\n");
            sb.append(methodAndUri).append("\n");
            sb.append("HTTP Status: ").append(response.getStatusCode()).append("\n");

            sb.append("\n");

            sb.append("Response Headers:\n");
            StringBuilder responseHeaders = new StringBuilder();
            response.getHeaders().forEach((key, value) -> responseHeaders.append(key).append(": ").append(value).append("\n"));
            sb.append(responseHeaders);

            sb.append("\n");
            sb.append("Response Body:\n");
            sb.append(new String(FileCopyUtils.copyToByteArray(response.getBody()))).append("\n");
            sb.append("\n");
            sb.append("Response Time: ").append(responseTime).append(" ms").append("\n");

            MDC.put(MDC_KEY_MSG_TYPE, "REST-OB-RES");
            MDC.put(MDC_KEY_TIME_TAKEN, stopWatch.getTime() + "");

            restMessageLogger.info(sb.toString());
            restStatsLogger.info("restStats - api[{}] timeTaken[{}] httpReturnStatusAsNum[{}]",
                    MDC.get(MDC_KEY_API), MDC.get(MDC_KEY_TIME_TAKEN), MDC.get(MDC_KEY_HTTP_STATUS));

            return response;

        } catch (IOException ioException) {

            stopWatch.stop();
            long responseTime = stopWatch.getTime();
            MDC.put(MDC_KEY_TIME_TAKEN, responseTime + "");
            MDC.put(MDC_KEY_HTTP_STATUS, String.valueOf(500));
            sb = new StringBuilder("REST Response:\n");
            sb.append(methodAndUri).append("\n");
            sb.append("HTTP Status: IOException");
            sb.append("\n\n");
            sb.append("Response Time: ").append(responseTime).append(" ms").append("\n");

            restMessageLogger.error(sb.toString(), ioException);
            restStatsLogger.error("restStats - api[{}] timeTaken[{}] httpReturnStatusAsNum[{}]",
                    MDC.get(MDC_KEY_API), MDC.get(MDC_KEY_TIME_TAKEN), MDC.get(MDC_KEY_HTTP_STATUS));

            throw ioException;

        } finally {
            if (previousContext != null) {
                MDC.setContextMap(previousContext);
            }
        }
    }
}
