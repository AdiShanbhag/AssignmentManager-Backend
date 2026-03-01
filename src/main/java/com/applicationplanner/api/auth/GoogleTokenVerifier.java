package com.applicationplanner.api.auth;

import com.applicationplanner.api.record.GoogleUserPayload;

public interface GoogleTokenVerifier {
    GoogleUserPayload verify(String idToken);

    GoogleUserPayload exchangeCode(String code);
}
