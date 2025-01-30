package com.cloudforge.invoice.delivery.constant;

public class Constants {
    // Security
    public static final String[] PUBLIC_URLS = { "/user/verify/password/**",
            "/user/login/**", "/user/verify/code/**",
            "/user/register/**", "/user/resetpassword/**", "/user/verify/account/**",
            "/user/refresh/token/**", "/user/image/**", "/user/new/password/**" };
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String[] PUBLIC_ROUTES = { "/user/new/password", "/user/login", "/user/verify/code", "/user/register", "/user/refresh/token", "/user/image" };
    public static final String HTTP_OPTIONS_METHOD = "OPTIONS";
    public static final String AUTHORITIES = "authorities";
    public static final String GET_ARRAYS_LLC = "GET_ARRAYS_LLC";
    public static final String CUSTOMER_MANAGEMENT_SERVICE = "CUSTOMER_MANAGEMENT_SERVICE";
    public static final long ACCESS_TOKEN_EXPIRATION_TIME = 432_000_000; //1_800_000;
    public static final long REFRESH_TOKEN_EXPIRATION_TIME = 432_000_000;
    public static final String TOKEN_CANNOT_BE_VERIFIED = "Token cannot be verified";

    // Request
    public static final String USER_AGENT_HEADER = "user-agent";
    public static final String X_FORWARDED_FOR_HEADER = "X-FORWARDED-FOR";

    // Date
    public static final String DATE_FORMAT = "yyyy-MM-dd hh:mm:ss";
}
