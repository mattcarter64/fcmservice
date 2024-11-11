package org.mcsoft.fcmservice.nimbusRestFactory;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.http.HttpHost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.net.Proxy;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

@Service
public class NimbusRestTemplateFactory {
    public static int NO_FLAGS = 0;
    public static int DONT_VALIDATE_SSL = 1;
    public static int USE_INTERNET_PROXY = 2;
    public static int USE_BASTION_PROXY = 4;
    public static int VERBOSE_LOGGING = 8;
    public static int FORCE_RESPONSE_AS_JSON = 16;
    public static int NO_CONNECTION_REUSE = 32;
    public static int DONT_VALIDATE_SSL_HOSTNAME = 64;
    @Value("${maxConnectionsPerRoute:600}")
    int maxConnectionsPerRoute;
    @Value("${bastionProxy:NOT_PROVIDED}")
    String bastionHost;
    @Value("${bastionPort:0}")
    int bastionPort;
    @Value("${internetProxyHost:pxyapp.proxy.att.com}")
    String internetProxyHost;
    @Value("${internetProxyPort:8080}")
    int internetProxyPort;

    public ClientHttpRequestFactory createHttpClient(int timeOutInMilliSecs, int flags) {

        try {
            SSLContext sslContext = SSLContext.getDefault();
            HostnameVerifier hostNameVerifier = SSLConnectionSocketFactory.getDefaultHostnameVerifier();

            if ((flags & DONT_VALIDATE_SSL) == DONT_VALIDATE_SSL) {
                sslContext = SSLContexts.custom().loadTrustMaterial(null, (X509Certificate[] chain, String authType) -> true).build();
                hostNameVerifier = NoopHostnameVerifier.INSTANCE;
            }

            if ((flags & DONT_VALIDATE_SSL_HOSTNAME) == DONT_VALIDATE_SSL_HOSTNAME) {
                hostNameVerifier = NoopHostnameVerifier.INSTANCE;
            }

            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", new SSLConnectionSocketFactory(sslContext, hostNameVerifier))
                .build();

            PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);

            connectionManager.setDefaultMaxPerRoute(maxConnectionsPerRoute);
            connectionManager.setMaxTotal(maxConnectionsPerRoute);

            HttpClientBuilder httpClient = HttpClients.custom().useSystemProperties()
                .setConnectionReuseStrategy(DefaultConnectionReuseStrategy.INSTANCE)
                .disableCookieManagement().setConnectionManager(connectionManager)
                //.setDefaultRequestConfig(RequestConfig.custom().setStaleConnectionCheckEnabled(true).build())
                .setMaxConnPerRoute(maxConnectionsPerRoute).setMaxConnTotal(maxConnectionsPerRoute * 2).setConnectionManagerShared(true);

            if ((flags & NO_CONNECTION_REUSE) == NO_CONNECTION_REUSE) {
                httpClient.setConnectionReuseStrategy(NoConnectionReuseStrategy.INSTANCE);
            }

            if ((flags & USE_INTERNET_PROXY) == USE_INTERNET_PROXY) {
                httpClient.setProxy(new HttpHost(internetProxyHost, internetProxyPort, Proxy.Type.HTTP.toString()));
            } else if ((flags & USE_BASTION_PROXY) == USE_BASTION_PROXY) {
                httpClient.setProxy(new HttpHost(bastionHost, bastionPort, Proxy.Type.HTTP.toString()));
            } else {
                httpClient.setProxy(null);
                httpClient.setRoutePlanner((target, request, context) -> new HttpRoute(target)); // Force Skipping Any proxy that may be set somewhere
            }

            return new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
//            new HttpComponentsClientHttpRequestFactory(httpClient.build()) {{
//                setConnectionRequestTimeout(timeOutInMilliSecs);
//                setConnectTimeout(timeOutInMilliSecs);
////                setReadTimeout(timeOutInMilliSecs);
//            }});
        } catch (Exception ex) {
            throw new RuntimeException("Should never see this", ex);
        }
    }

    public RestTemplate createRestTemplate(int timeOutInMilliSecs, int flags, List<SecurityProvider> securityProviders) {
        try {
            RestTemplate template = new RestTemplate(createHttpClient(timeOutInMilliSecs, flags));
            List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();

            if ((flags & FORCE_RESPONSE_AS_JSON) == FORCE_RESPONSE_AS_JSON) {
                interceptors.add(new MediaOverrideInterceptor(MediaType.APPLICATION_JSON));
            }

            if ((flags & VERBOSE_LOGGING) == VERBOSE_LOGGING) {
                interceptors.add(new LoggingRequestInterceptor());
            }

            if (securityProviders != null)
                interceptors.addAll(securityProviders);

            template.setInterceptors(interceptors);

            //If there is an xml converter, move it to the end of the list so the default will be json.  I hate spring magic
            List<HttpMessageConverter<?>> messageConverters = template.getMessageConverters();
            messageConverters.sort((o1, o2) -> {
                if (o1 instanceof MappingJackson2XmlHttpMessageConverter) {
                    return 1;
                } else if (o2 instanceof MappingJackson2XmlHttpMessageConverter) {
                    return -1;
                } else {
                    return 0;
                }
            });
            template.setMessageConverters(messageConverters);

            return template;
        } catch (Exception ex) {
            throw new RuntimeException("Should never see this", ex);
        }
    }

    public RestTemplate createRestTemplate(int timeOutInMilliSecs, List<SecurityProvider> securityProviders) {
        return createRestTemplate(timeOutInMilliSecs, NO_FLAGS, securityProviders);
    }

    public RestTemplate createRestTemplate(int timeOutInMilliSecs, int flags) {
        return createRestTemplate(timeOutInMilliSecs, flags, null);
    }

    public RestTemplate createRestTemplate(int timeOutInMilliSecs) {
        return createRestTemplate(timeOutInMilliSecs, NO_FLAGS, null);
    }

    public RestTemplate createInternalTemplate(int timeOutInMilliSecs, boolean shouldLog) {
        return createRestTemplate(timeOutInMilliSecs, DONT_VALIDATE_SSL | (shouldLog ? VERBOSE_LOGGING : 0));
    }
}
