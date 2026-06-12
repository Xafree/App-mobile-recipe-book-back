package fr.gymgod.app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gymgod.common.constants.SecurityConstants;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ConcurrentHashMap<String, Bucket> bucketRegistry = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!isRateLimitedEndpoint(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        Bucket bucket = bucketRegistry.computeIfAbsent(clientIp, ip -> createBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP={} on {}", clientIp, request.getRequestURI());
            writeTooManyRequestsResponse(response);
        }
    }

    private boolean isRateLimitedEndpoint(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) return false;
        String uri = request.getRequestURI();
        return uri.equals("/api/login") || uri.equals("/api/auth/register");
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(SecurityConstants.RATE_LIMIT_MAX_REQUESTS)
                .refillGreedy(SecurityConstants.RATE_LIMIT_MAX_REQUESTS,
                        Duration.ofSeconds(SecurityConstants.RATE_LIMIT_WINDOW_SECONDS))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private void writeTooManyRequestsResponse(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        OBJECT_MAPPER.writeValue(response.getWriter(), Map.of(
                "error", "too_many_requests",
                "message", "Too many attempts. Please wait before trying again."));
    }
}
