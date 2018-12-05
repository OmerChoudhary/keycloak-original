/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.crypto;

import org.keycloak.common.VerificationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

public class AsymmetricSignatureProvider implements SignatureProvider {

    private final KeycloakSession session;
    private final String algorithm;

    public AsymmetricSignatureProvider(KeycloakSession session, String algorithm) {
        this.session = session;
        this.algorithm = algorithm;
    }

    @Override
    public SignatureSignerContext signer(RealmModel realm) throws SignatureException {
        return new ServerAsymmetricSignatureSignerContext(session, realm, algorithm);
    }

    @Override
    public SignatureVerifierContext verifier(RealmModel realm, String kid) throws VerificationException {
        return new ServerAsymmetricSignatureVerifierContext(session, realm, kid, algorithm);
    }

}
