package com.infinite.common.config.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
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

import static com.infinite.common.config.handler.GlobalExceptionHandler.HANDLED_EXCEPTION_ATTRIBUTE;
import static com.infinite.common.util.Constants.SERVICE_NAME;

@Component
@Order(2)
@Slf4j
public class AccessFilter extends OncePerRequestFilter {
    private static final String EVENT = "event";
    private static final String METHOD = "method";
    private static final String PATH = "path";
    private static final String PARENT_SPAN_ID = "parentSpanId";
    private static final String FROM = "from";
    private static final String HEADERS = "headers";
    private static final String PARAMS = "params";
    private static final String BODY = "body";
    private static final String STATUS = "status";
    private static final String DURATION_MS = "durationMs";
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

        putRequestPayloadToMdc(wrappedRequest);
        
        // Temporarily set event="request" for request log only
        String originalEvent = MDC.get(EVENT);
        MDC.put(EVENT, "request");
        log.info(buildRequestLogMessage());
        
        // Restore original event (should be "debug" from DebugEventFilter)
        if (originalEvent != null) {
            MDC.put(EVENT, originalEvent);
        } else {
            MDC.remove(EVENT);
        }
        
        clearRequestPayloadMdc();

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long durationMs = System.currentTimeMillis() - startTime;

            // Temporarily set event="response" for response log only
            String currentEvent = MDC.get(EVENT);
            MDC.put(EVENT, "response");
            MDC.put(DURATION_MS, String.valueOf(durationMs));
            logResponse(wrappedRequest, wrappedResponse);

            wrappedResponse.copyBodyToResponse();
            clearMdc();
        }
    }

    private void logResponse(HttpServletRequest request, ContentCachingResponseWrapper response) {
        int status = resolveStatus(response);
        Throwable throwable = extractThrowable(request);
        String responseMessage = resolveResponseMessage(response, status, throwable);

        MDC.put(STATUS, String.valueOf(status));

        if (status >= 500) {
            if (throwable != null) {
                log.error(responseMessage, throwable);
            } else {
                log.error(responseMessage);
            }
        } else if (status >= 400) {
            if (throwable != null) {
                log.warn(responseMessage, throwable);
            } else {
                log.warn(responseMessage);
            }
        } else {
            if (throwable != null) {
                log.info(responseMessage, throwable);
            } else {
                log.info(responseMessage);
            }
        }
    }

    private int resolveStatus(ContentCachingResponseWrapper response) {
        int status = response.getStatus();
        return status > 0 ? status : HttpServletResponse.SC_OK;
    }

    private String resolveResponseMessage(ContentCachingResponseWrapper response, int status, Throwable throwable) {
        String throwableMessage = extractThrowableMessage(throwable);
        if (StringUtils.hasText(throwableMessage)) {
            return throwableMessage;
        }

        ResponseBodyDetail responseBodyDetail = extractResponseBodyDetail(response);
        if (responseBodyDetail != null && StringUtils.hasText(responseBodyDetail.message())) {
            return responseBodyDetail.message();
        }

        return buildDefaultResponseMessage(status);
    }

    private void putRequestPayloadToMdc(ContentCachingRequestWrapper request) {
        putJsonToMdc(PARAMS, normalizeParams(request.getParameterMap()));
        putJsonToMdc(HEADERS, extractHeaders(request));
        putIfHasText(BODY, extractRequestBody(request));
    }

    private void clearRequestPayloadMdc() {
        MDC.remove(PARAMS);
        MDC.remove(HEADERS);
        MDC.remove(BODY);
    }

    private void clearMdc() {
        clearRequestPayloadMdc();
        MDC.remove(EVENT);
        MDC.remove(METHOD);
        MDC.remove(PATH);
        MDC.remove(PARENT_SPAN_ID);
        MDC.remove(FROM);
        MDC.remove(STATUS);
        MDC.remove(DURATION_MS);
    }

    private String buildRequestLogMessage() {
        String source = MDC.get(FROM);
        if (!StringUtils.hasText(source)) {
            return "API Request";
        }
        return source + " call to " + SERVICE_NAME;
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

    private void putJsonToMdc(String key, Object value) {
        if (value == null) {
            return;
        }

        if (value instanceof Map<?, ?> map && map.isEmpty()) {
            return;
        }

        try {
            MDC.put(key, objectMapper.writeValueAsString(value));
        } catch (Exception ex) {
            MDC.put(key, String.valueOf(value));
        }
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

    private ResponseBodyDetail extractResponseBodyDetail(ContentCachingResponseWrapper response) {
        byte[] content = response.getContentAsByteArray();
        if (content.length == 0) {
            return null;
        }

        String responseBody = readPayload(content, resolveResponseEncoding(response), response.getContentType(), false);
        if (!StringUtils.hasText(responseBody)) {
            return null;
        }

        if (isJsonContentType(response.getContentType())) {
            try {
                JsonNode body = objectMapper.readTree(responseBody);
                JsonNode messageNode = body.get("message");
                if (messageNode != null && !messageNode.isNull()) {
                    String message = messageNode.asText();
                    if (StringUtils.hasText(message)) {
                        return new ResponseBodyDetail(message);
                    }
                }
            } catch (Exception ignored) {
                return new ResponseBodyDetail(responseBody);
            }
        }

        return new ResponseBodyDetail(responseBody);
    }

    private Throwable extractThrowable(HttpServletRequest request) {
        Object handledException = request.getAttribute(HANDLED_EXCEPTION_ATTRIBUTE);
        if (handledException instanceof Throwable throwable) {
            return throwable;
        }

        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        if (exception instanceof Throwable throwable) {
            return throwable;
        }

        Object defaultError = request
                .getAttribute("org.springframework.boot.web.servlet.error.DefaultErrorAttributes.ERROR");
        if (defaultError instanceof Throwable throwable) {
            return throwable;
        }

        return null;
    }

    private String resolveResponseEncoding(ContentCachingResponseWrapper response) {
        String contentType = response.getContentType();
        if (isJsonContentType(contentType)) {
            return StandardCharsets.UTF_8.name();
        }

        String encoding = response.getCharacterEncoding();
        if (StringUtils.hasText(encoding)
                && !StandardCharsets.ISO_8859_1.name().equalsIgnoreCase(encoding)) {
            return encoding;
        }

        if (StringUtils.hasText(contentType) && contentType.toLowerCase().contains("charset=utf-8")) {
            return StandardCharsets.UTF_8.name();
        }

        return StandardCharsets.UTF_8.name();
    }

    private String extractThrowableMessage(Throwable throwable) {
        if (throwable == null) {
            return null;
        }

        String message = throwable.getMessage();
        return StringUtils.hasText(message) ? message : null;
    }

    private String buildDefaultResponseMessage(int status) {
        HttpStatus httpStatus = HttpStatus.resolve(status);
        if (httpStatus != null) {
            return httpStatus.getReasonPhrase();
        }
        return "http request completed";
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

    private record ResponseBodyDetail(String message) {
    }
}
