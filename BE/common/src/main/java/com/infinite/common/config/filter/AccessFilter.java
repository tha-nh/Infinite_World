package com.infinite.common.config.filter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Order(2)
@Slf4j
public class AccessFilter extends OncePerRequestFilter {
    private static final String EVENT = "event";
    private static final String METHOD = "method";
    private static final String PATH = "path";
    private static final String PARENT_SPAN_ID = "parentSpanId";
    private static final String FROM = "from";
    private static final String FROM_HEADER = "X-From";
    private static final String SPAN_ID_HEADER = "X-Span-Id";
    private static final int MAX_PAYLOAD_LENGTH = 5000;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, MAX_PAYLOAD_LENGTH);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        MDC.put(METHOD, request.getMethod());
        MDC.put(PATH, request.getRequestURI());

        putIfHasText(PARENT_SPAN_ID, request.getHeader(SPAN_ID_HEADER));
        putIfHasText(FROM, request.getHeader(FROM_HEADER));

        try {
            MDC.put(EVENT, "request");
            log.info(buildRequestMessage(wrappedRequest));

            filterChain.doFilter(wrappedRequest, wrappedResponse);

            long durationMs = System.currentTimeMillis() - startTime;
            MDC.put(EVENT, "response");
            logResponse(wrappedResponse, durationMs);
        } finally {
            wrappedResponse.copyBodyToResponse();
            MDC.remove(EVENT);
            MDC.remove(METHOD);
            MDC.remove(PATH);
            MDC.remove(PARENT_SPAN_ID);
            MDC.remove(FROM);
        }
    }

    private void logResponse(ContentCachingResponseWrapper response, long durationMs) {
        String responseMessage = extractResponseMessage(response);

        if (StringUtils.hasText(responseMessage)) {
            log.info("status={} durationMs={} message={}",
                    response.getStatus(), durationMs, responseMessage);
            return;
        }

        log.info("completed http request status={} durationMs={}",
                response.getStatus(), durationMs);
    }

    private String buildRequestMessage(ContentCachingRequestWrapper request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        Map<String, String[]> params = request.getParameterMap();
        Map<String, String> headers = extractHeaders(request);
        String body = extractRequestBody(request);

        if (!CollectionUtils.isEmpty(params)) {
            payload.put("params", normalizeParams(params));
        }
        if (!CollectionUtils.isEmpty(headers)) {
            payload.put("headers", headers);
        }
        if (StringUtils.hasText(body)) {
            payload.put("body", body);
        }

        if (payload.isEmpty()) {
            return "incoming http request";
        }

        try {
            return "incoming http request " + objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            return "incoming http request " + payload;
        }
    }

    private Map<String, Object> normalizeParams(Map<String, String[]> params) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        params.forEach((key, value) -> {
            if (value == null) {
                normalized.put(key, null);
            } else if (value.length == 1) {
                normalized.put(key, value[0]);
            } else {
                normalized.put(key, Arrays.asList(value));
            }
        });
        return normalized;
    }

    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        var headerNames = request.getHeaderNames();
        while (headerNames != null && headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            if (StringUtils.hasText(headerValue)) {
                headers.put(headerName, headerValue);
            }
        }
        return headers;
    }

    private String extractRequestBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content.length == 0) {
            return null;
        }
        return readPayload(content, request.getCharacterEncoding(), request.getContentType(), true);
    }

    private String extractResponseMessage(ContentCachingResponseWrapper response) {
        byte[] content = response.getContentAsByteArray();
        if (content.length == 0) {
            return null;
        }

        String responseBody = readPayload(content, response.getCharacterEncoding(), response.getContentType(), false);
        if (!StringUtils.hasText(responseBody)) {
            return null;
        }

        if (isJsonContentType(response.getContentType())) {
            try {
                Map<String, Object> body = objectMapper.readValue(responseBody, new TypeReference<>() {
                });
                Object message = body.get("message");
                if (message != null && StringUtils.hasText(String.valueOf(message))) {
                    return String.valueOf(message);
                }
            } catch (Exception ignored) {
                return responseBody;
            }
        }

        return responseBody;
    }

    private String readPayload(byte[] content, String encoding, String contentType, boolean request) {
        if (!isReadableContentType(contentType)) {
            return null;
        }

        Charset charset = StandardCharsets.UTF_8;
        if (StringUtils.hasText(encoding)) {
            try {
                charset = Charset.forName(encoding);
            } catch (Exception ignored) {
                charset = StandardCharsets.UTF_8;
            }
        }

        String payload = new String(content, charset);
        if (!StringUtils.hasText(payload)) {
            return null;
        }

        String sanitized = payload.trim();
        if (!StringUtils.hasText(sanitized)) {
            return null;
        }

        if (sanitized.length() > MAX_PAYLOAD_LENGTH) {
            return sanitized.substring(0, MAX_PAYLOAD_LENGTH) +
                    "...(" + (request ? "request" : "response") + "-body-truncated)";
        }

        return sanitized;
    }

    private boolean isReadableContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return true;
        }

        try {
            MediaType mediaType = MediaType.parseMediaType(contentType);
            return MediaType.APPLICATION_JSON.includes(mediaType)
                    || MediaType.APPLICATION_XML.includes(mediaType)
                    || MediaType.TEXT_PLAIN.includes(mediaType)
                    || MediaType.TEXT_HTML.includes(mediaType)
                    || MediaType.APPLICATION_FORM_URLENCODED.includes(mediaType)
                    || "text".equalsIgnoreCase(mediaType.getType());
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean isJsonContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return false;
        }

        try {
            MediaType mediaType = MediaType.parseMediaType(contentType);
            return MediaType.APPLICATION_JSON.includes(mediaType)
                    || mediaType.getSubtype().toLowerCase().contains("json");
        } catch (Exception ex) {
            return false;
        }
    }

    private void putIfHasText(String key, String value) {
        if (StringUtils.hasText(value)) {
            MDC.put(key, value);
        }
    }
}
