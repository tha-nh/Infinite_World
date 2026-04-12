package com.infinite.common.config.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.io.IOException;
import java.util.Locale;

@Component
public class LocaleFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        LocaleResolver resolver = RequestContextUtils.getLocaleResolver(req);
        if (resolver != null) {
            Locale locale = resolver.resolveLocale(req);
            LocaleContextHolder.setLocale(locale);
        }
        chain.doFilter(request, response);
    }
}