package com.infinite.common.config.handler;

import com.infinite.common.constant.StatusCode;
import com.infinite.common.dto.response.ApiResponse;
import com.infinite.common.dto.response.Response;
import com.infinite.common.exception.AppException;
import com.infinite.common.exception.ExtendedRuntimeException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    public static final String HANDLED_EXCEPTION_ATTRIBUTE = GlobalExceptionHandler.class.getName()
            + ".HANDLED_EXCEPTION";

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Object>> handleAppException(AppException ex, HttpServletRequest request) {
        StatusCode statusCode = ex.getStatusCode() != null ? ex.getStatusCode() : StatusCode.INTERNAL_ERROR;
        String message = resolveMessage(ex.getMessage(), statusCode);

        request.setAttribute(HANDLED_EXCEPTION_ATTRIBUTE, ex);

        ApiResponse<Object> response = ApiResponse.builder()
                .code(statusCode.getCode())
                .message(message)
                .result(ex.getResult())
                .build();

        return ResponseEntity.status(statusCode.getHttpStatusCode()).body(response);
    }

    @ExceptionHandler(ExtendedRuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleExtendedRuntimeException(
            ExtendedRuntimeException ex,
            HttpServletRequest request) {
        String message = resolveMessage(ex.getCode(), StatusCode.BAD_REQUEST);

        request.setAttribute(HANDLED_EXCEPTION_ATTRIBUTE, ex);

        ApiResponse<Object> response = ApiResponse.builder()
                .code(StatusCode.BAD_REQUEST.getCode())
                .message(message)
                .build();

        return ResponseEntity.status(StatusCode.BAD_REQUEST.getHttpStatusCode()).body(response);
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ApiResponse<Object>> handleBadRequestExceptions(Exception ex, HttpServletRequest request) {
        String message = resolveBadRequestMessage(ex);

        request.setAttribute(HANDLED_EXCEPTION_ATTRIBUTE, ex);

        ApiResponse<Object> response = ApiResponse.builder()
                .code(StatusCode.BAD_REQUEST.getCode())
                .message(message)
                .build();

        return ResponseEntity.status(StatusCode.BAD_REQUEST.getHttpStatusCode()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception ex, HttpServletRequest request) {
        String message = resolveMessage(ex.getMessage(), StatusCode.INTERNAL_ERROR);

        request.setAttribute(HANDLED_EXCEPTION_ATTRIBUTE, ex);

        ApiResponse<Object> response = ApiResponse.builder()
                .code(StatusCode.INTERNAL_ERROR.getCode())
                .message(message)
                .build();

        return ResponseEntity.status(StatusCode.INTERNAL_ERROR.getHttpStatusCode()).body(response);
    }

    private String resolveBadRequestMessage(Exception ex) {
        if (ex instanceof MethodArgumentNotValidException methodArgumentNotValidException
                && methodArgumentNotValidException.getBindingResult().hasFieldErrors()) {
            String defaultMessage = methodArgumentNotValidException.getBindingResult()
                    .getFieldErrors()
                    .getFirst()
                    .getDefaultMessage();
            if (StringUtils.hasText(defaultMessage)) {
                return defaultMessage;
            }
        }

        if (ex instanceof BindException bindException && bindException.getBindingResult().hasFieldErrors()) {
            String defaultMessage = bindException.getBindingResult().getFieldErrors().getFirst().getDefaultMessage();
            if (StringUtils.hasText(defaultMessage)) {
                return defaultMessage;
            }
        }

        if (ex instanceof ConstraintViolationException constraintViolationException
                && !constraintViolationException.getConstraintViolations().isEmpty()) {
            String message = constraintViolationException.getConstraintViolations().iterator().next().getMessage();
            if (StringUtils.hasText(message)) {
                return message;
            }
        }

        return resolveMessage(ex.getMessage(), StatusCode.BAD_REQUEST);
    }

    private String resolveMessage(String message, StatusCode fallbackStatus) {
        if (StringUtils.hasText(message)) {
            String translatedMessage = Response.message(message);
            if (StringUtils.hasText(translatedMessage) && !translatedMessage.equals(message)) {
                return translatedMessage;
            }
            return message;
        }

        return Response.message(fallbackStatus);
    }
}
