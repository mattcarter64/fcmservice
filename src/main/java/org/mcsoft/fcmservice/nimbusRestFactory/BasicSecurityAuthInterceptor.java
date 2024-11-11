package org.mcsoft.fcmservice.nimbusRestFactory;

import org.springframework.http.client.support.BasicAuthenticationInterceptor ;

public class BasicSecurityAuthInterceptor extends BasicAuthenticationInterceptor  implements SecurityProvider {
    public BasicSecurityAuthInterceptor(String username, String password) {
        super(username, password);
    }
}
