/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.credential;

import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.client.Origin;
import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WebAuthnCredentialProviderFactory implements CredentialProviderFactory<WebAuthnCredentialProvider>, EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "keycloak-webauthn";

    private ObjectConverter converter;
    private Config.Scope config;

    @Override
    public void init(Config.Scope config) {
        this.config = config;
    }

    @Override
    public CredentialProvider create(KeycloakSession session) {
        Set<Origin> origins = Optional.ofNullable(config.getArray("fido2-origins"))
                .map(Arrays::stream)
                .orElseGet(Stream::empty)
                .map(Origin::new)
                .collect(Collectors.toSet());
        return new WebAuthnCredentialProvider(session, createOrGetObjectConverter(), origins);
    }

    private ObjectConverter createOrGetObjectConverter() {
        if (converter == null) {
            synchronized (this) {
                if (converter == null) {
                    converter = new ObjectConverter();
                }
            }
        }
        return converter;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.WEB_AUTHN);
    }
}
