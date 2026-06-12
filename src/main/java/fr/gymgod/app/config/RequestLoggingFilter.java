package fr.gymgod.app.config;

import fr.gymgod.common.constants.LoggingConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        MDC.put(LoggingConstants.MDC_REQUEST_ID, requestId);
        response.setHeader(LoggingConstants.HEADER_REQUEST_ID, requestId);

        String method  = request.getMethod();
        String uri     = request.getRequestURI();
        long   startMs = System.currentTimeMillis();

        try {
            filterChain.doFilter(request, response);
        } finally {
            Object userId = request.getAttribute(LoggingConstants.REQUEST_ATTR_USER_ID);
            MDC.put(LoggingConstants.MDC_USER_ID, userId != null ? (String) userId : "anon");
            logCompletion(method, uri, response.getStatus(), System.currentTimeMillis() - startMs);
            MDC.clear();
        }
    }

    private void logCompletion(String method, String uri, int status, long durationMs) {
        if (isSilentUri(uri)) return;
        String msg = "{} {} → {} ({}ms)";
        if (status >= 500)      log.error(msg, method, uri, status, durationMs);
        else if (status >= 400) log.warn(msg, method, uri, status, durationMs);
        else                    log.info(msg, method, uri, status, durationMs);
    }

    private boolean isSilentUri(String uri) {
        for (String prefix : LoggingConstants.SILENT_URI_PREFIXES) {
            if (uri.startsWith(prefix)) return true;
        }
        return false;
    }
}
