// Copyright (c) 2021 The Trade Desk, Inc
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice,
//    this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

package com.uid2.operator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.jupiter.api.Test;

import com.uid2.operator.model.IdentityScope;
import com.uid2.shared.auth.Role;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;

public class EUIDOperatorVerticleTest extends UIDOperatorVerticleTest {
    @Override
    public void setupConfig(JsonObject config) {
        config.put("identity_scope", getIdentityScope().toString());
        config.put("advertising_token_v3", true);
        config.put("refresh_token_v3", true);
        config.put("identity_v3", useIdentityV3());
    }

    @Override
    protected boolean useIdentityV3() { return true; }
    @Override
    protected IdentityScope getIdentityScope() { return IdentityScope.EUID; }
    @Override
    protected void addAdditionalTokenGenerateParams(JsonObject payload) {
        if (payload != null && !payload.containsKey("tcf_consent_string")) {
            payload.put("tcf_consent_string", "CPehNtWPehNtWABAMBFRACBoALAAAEJAAIYgAKwAQAKgArABAAqAAA");
        }
    }

    @Test
    void badRequestOnInvalidTcfConsent(Vertx vertx, VertxTestContext testContext) {
        final int clientSiteId = 201;
        fakeAuth(clientSiteId, Role.GENERATOR);
        setupSalts();
        setupKeys();
        
        final String emailAddress = "test@uid2.com";
        final JsonObject v2Payload = new JsonObject();
        v2Payload.put("email", emailAddress);
        v2Payload.put("tcf_consent_string", "invalid_consent_string");
        sendTokenGenerate("v2", vertx, "", v2Payload, 400, json -> {
            testContext.completeNow();
        });
    }

    @Test
    void noContentOnInsufficientTcfConsent(Vertx vertx, VertxTestContext testContext) {
        final int clientSiteId = 201;
        fakeAuth(clientSiteId, Role.GENERATOR);
        setupSalts();
        setupKeys();

        final String emailAddress = "test@uid2.com";
        final JsonObject v2Payload = new JsonObject();
        v2Payload.put("email", emailAddress);
        // this TCString is missing consent for purpose #1
        v2Payload.put("tcf_consent_string", "CPehXK9PehXK9ABAMBFRACBoADAAAEJAAIYgAKwAQAKgArABAAqAAA");
        sendTokenGenerate("v2", vertx, "", v2Payload, 200, json -> {
            assertFalse(json.containsKey("body"));
            assertEquals("insufficient_user_consent", json.getString("status"));
            testContext.completeNow();
        });
    }
}
