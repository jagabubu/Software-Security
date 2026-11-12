package com.northbridge.casevault.security;

import com.northbridge.casevault.service.AuditService;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class LoginAuditListener {

    private final AuditService auditService;
    private final LoginAttemptService loginAttemptService;

    public LoginAuditListener(AuditService auditService, LoginAttemptService loginAttemptService) {
        this.auditService = auditService;
        this.loginAttemptService = loginAttemptService;
    }

    @EventListener
    public void onLoginSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        loginAttemptService.reset(username);
        auditService.record(username, "LOGIN", "session", "SUCCESS");
    }

    @EventListener
    public void onLoginFailure(AbstractAuthenticationFailureEvent event) {
        String username = event.getAuthentication().getName();
        loginAttemptService.recordFailure(username);
        auditService.record(username, "LOGIN", "session", "FAILURE");
    }
}
