package fr.gymgod.common.constants;

public final class LoggingConstants {

    public static final String MDC_REQUEST_ID       = "requestId";
    public static final String MDC_USER_ID          = "userId";
    public static final String HEADER_REQUEST_ID    = "X-Request-Id";
    public static final String REQUEST_ATTR_USER_ID = "_logging_userId";

    public static final String[] SILENT_URI_PREFIXES = {
            "/actuator/health",
            "/swagger-ui",
            "/v3/api-docs"
    };

    private LoggingConstants() {}
}
