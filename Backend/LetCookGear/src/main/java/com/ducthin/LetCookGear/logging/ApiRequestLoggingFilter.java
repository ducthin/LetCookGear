package com.ducthin.LetCookGear.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Slf4j
public class ApiRequestLoggingFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startNanos = System.nanoTime();
        String requestId = UUID.randomUUID().toString();
        response.setHeader(REQUEST_ID_HEADER, requestId);

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String target = query == null || query.isBlank() ? uri : uri + "?" + query;
        String maskedTarget = LogSanitizer.sanitizeText(target);
        String maskedAuthorization = LogSanitizer.maskAuthorizationHeader(request.getHeader("Authorization"));

        log.info("API_IN requestId={} method={} path={} remoteIp={} authorization={}",
                requestId,
                method,
            maskedTarget,
            request.getRemoteAddr(),
            maskedAuthorization);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
            int status = response.getStatus();
            String user = resolveCurrentUser();

            if (status >= 500) {
                log.error("API_OUT requestId={} method={} path={} status={} durationMs={} user={}",
                        requestId,
                        method,
                    maskedTarget,
                        status,
                        durationMs,
                        user);
            } else if (status >= 400) {
                log.warn("API_OUT requestId={} method={} path={} status={} durationMs={} user={}",
                        requestId,
                        method,
                    maskedTarget,
                        status,
                        durationMs,
                        user);
            } else {
                log.info("API_OUT requestId={} method={} path={} status={} durationMs={} user={}",
                        requestId,
                        method,
                    maskedTarget,
                        status,
                        durationMs,
                        user);
            }
        }
    }

    private String resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return "anonymous";
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }

        if (principal == null) {
            return "anonymous";
        }

        return principal.toString();
    }
}
