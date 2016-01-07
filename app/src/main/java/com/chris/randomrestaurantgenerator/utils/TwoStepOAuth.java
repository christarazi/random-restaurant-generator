package com.chris.randomrestaurantgenerator.utils;

import com.chris.randomrestaurantgenerator.BuildConfig;

import org.scribe.builder.api.DefaultApi10a;
import org.scribe.model.Token;

/**
 * Generic service provider for two-step OAuth10a.
 */
public class TwoStepOAuth extends DefaultApi10a {

    private static final String CONSUMER_KEY = BuildConfig.API_CONSUMER_KEY;
    private static final String CONSUMER_SECRET = BuildConfig.API_CONSUMER_SECRET;
    private static final String TOKEN = BuildConfig.API_TOKEN;
    private static final String TOKEN_SECRET = BuildConfig.API_TOKEN_SECRET;

    public static String getConsumerKey() {
        return CONSUMER_KEY;
    }

    public static String getConsumerSecret() {
        return CONSUMER_SECRET;
    }

    public static String getToken() {
        return TOKEN;
    }

    public static String getTokenSecret() {
        return TOKEN_SECRET;
    }

    @Override
    public String getAccessTokenEndpoint() {
        return null;
    }

    @Override
    public String getAuthorizationUrl(Token arg0) {
        return null;
    }

    @Override
    public String getRequestTokenEndpoint() {
        return null;
    }
}
