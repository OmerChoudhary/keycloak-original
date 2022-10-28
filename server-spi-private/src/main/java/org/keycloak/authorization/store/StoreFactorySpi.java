/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.authorization.store;

import org.keycloak.common.Feature;
import org.keycloak.common.Profile;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class StoreFactorySpi implements Spi {

    public static final String NAME = "authorizationPersister";

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return StoreFactory.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return AuthorizationStoreFactory.class;
    }

    @Override
    public boolean isEnabled() {
        return Profile.isFeatureEnabled(Feature.AUTHORIZATION);
    }
}
