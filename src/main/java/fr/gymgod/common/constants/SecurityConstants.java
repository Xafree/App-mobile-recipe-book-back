package fr.gymgod.common.constants;

public final class SecurityConstants {

    public static final int  BCRYPT_STRENGTH              = 12;
    public static final int  RATE_LIMIT_MAX_REQUESTS      = 5;
    public static final long RATE_LIMIT_WINDOW_SECONDS    = 60L;

    public static final String GOOGLE_TOKENINFO_URL       = "https://oauth2.googleapis.com/tokeninfo?id_token=";
    public static final String APPLE_JWKS_URL             = "https://appleid.apple.com/auth/keys";
    public static final String APPLE_ISSUER               = "https://appleid.apple.com";
    public static final long   APPLE_JWKS_CACHE_TTL_MS    = 3_600_000L;

    private SecurityConstants() {}
}
