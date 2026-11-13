package com.northbridge.casevault.security;

import com.northbridge.casevault.model.User;
import com.northbridge.casevault.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// Runs on every request. If the logged-in account has been disabled (or deleted),
// the session is destroyed straight away instead of waiting for it to expire.
public class DisabledAccountFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public DisabledAccountFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {

            boolean stillAllowed = userRepository.findByUsername(authentication.getName())
                    .map(User::isEnabled)
                    .orElse(false);

            if (!stillAllowed) {
                SecurityContextHolder.clearContext();
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                response.sendRedirect("/login?error");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}