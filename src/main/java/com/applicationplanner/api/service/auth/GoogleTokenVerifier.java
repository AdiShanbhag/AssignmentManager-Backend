package com.applicationplanner.api.service.auth;

import com.applicationplanner.api.record.GoogleUserPayload;

public interface GoogleTokenVerifier {
    GoogleUserPayload verify(String idToken);
}
