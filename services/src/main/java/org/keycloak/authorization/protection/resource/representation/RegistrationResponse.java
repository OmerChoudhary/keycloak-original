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

package org.keycloak.authorization.protection.resource.representation;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class RegistrationResponse {

    private final UmaResourceRepresentation resourceDescription;

    public RegistrationResponse(UmaResourceRepresentation resourceDescription) {
        this.resourceDescription = resourceDescription;
    }

    public RegistrationResponse() {
        this(null);
    }

    @JsonUnwrapped
    public UmaResourceRepresentation getResourceDescription() {
        return this.resourceDescription;
    }

    public String getId() {
        if (this.resourceDescription != null) {
            return this.resourceDescription.getId();
        }

        return null;
    }
}
