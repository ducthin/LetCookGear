package com.ducthin.LetCookGear.logging;

import java.util.regex.Pattern;

public final class LogSanitizer {

    private static final String MASK = "***";

    private static final Pattern JSON_SENSITIVE_VALUE = Pattern.compile(
            "(?i)\\\"(password|token|access_token|refresh_token|authorization)\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");

    private static final Pattern QUERY_SENSITIVE_VALUE = Pattern.compile(
            "(?i)(password|token|access_token|refresh_token|authorization)=([^&\\s]*)");

    private static final Pattern BEARER_TOKEN = Pattern.compile("(?i)^Bearer\\s+.+$");

    private LogSanitizer() {
    }

    public static String sanitizeText(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        String sanitized = JSON_SENSITIVE_VALUE.matcher(value).replaceAll("\"$1\":\"" + MASK + "\"");
        sanitized = QUERY_SENSITIVE_VALUE.matcher(sanitized).replaceAll("$1=" + MASK);
        return sanitized;
    }

    public static String maskAuthorizationHeader(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return "none";
        }

        if (BEARER_TOKEN.matcher(authorizationHeader).matches()) {
            return "Bearer " + MASK;
        }

        return MASK;
    }
}
