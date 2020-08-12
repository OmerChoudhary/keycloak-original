/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.validation;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Denotes a dedicated ValidationContext in which certain {@link Validation} rules should be applied, e.g. during User Registration,
 * User Profile change, Client Registration, Realm Definition, Identity Provider Configuration, etc.
 * {@link Validation Validation's} can be associated with different {@link ValidationContextKey}.
 * <p>
 * Users can create custom {@link ValidationContextKey ValidationContextKey's} by implementing this interface.
 * It is recommended that custom {@link ValidationContextKey} implementations are singletons.
 */
public interface ValidationContextKey {

    ValidationContextKey DEFAULT_CONTEXT_KEY = new BuiltInValidationContextKey("", null);

    ValidationContextKey REALM_CONTEXT_KEY =
            new BuiltInValidationContextKey("realm", DEFAULT_CONTEXT_KEY);

    ValidationContextKey CLIENT_CONTEXT_KEY =
            new BuiltInValidationContextKey("client", DEFAULT_CONTEXT_KEY);

    ValidationContextKey USER_CONTEXT_KEY =
            new BuiltInValidationContextKey("user", DEFAULT_CONTEXT_KEY);

    ValidationContextKey USER_RESOURCE_CONTEXT_KEY =
            new BuiltInValidationContextKey("user.resource", USER_CONTEXT_KEY);

    ValidationContextKey USER_PROFILE_CONTEXT_KEY =
            new BuiltInValidationContextKey("user.profile", USER_CONTEXT_KEY);

    ValidationContextKey USER_REGISTRATION_CONTEXT_KEY =
            new BuiltInValidationContextKey("user.registration", USER_CONTEXT_KEY);

    ValidationContextKey USER_PROFILE_REGISTRATION_CONTEXT_KEY =
            new BuiltInValidationContextKey("user.profile.registration", USER_CONTEXT_KEY);

    ValidationContextKey USER_PROFILE_IDP_REVIEW_CONTEXT_KEY =
            new BuiltInValidationContextKey("user.profile.idp_review", USER_CONTEXT_KEY);

    List<ValidationContextKey> ALL_CONTEXT_KEYS = Collections.unmodifiableList(Arrays.asList(

            DEFAULT_CONTEXT_KEY,

            REALM_CONTEXT_KEY,

            CLIENT_CONTEXT_KEY,

            USER_CONTEXT_KEY,

            USER_RESOURCE_CONTEXT_KEY,
            USER_PROFILE_CONTEXT_KEY,

            USER_PROFILE_IDP_REVIEW_CONTEXT_KEY,
            USER_PROFILE_REGISTRATION_CONTEXT_KEY,
            USER_REGISTRATION_CONTEXT_KEY
    ));

    String getName();

    ValidationContextKey getParent();

    default List<ValidationActionKey> getActions() {
        return Collections.singletonList(ValidationActionKey.DEFAULT);
    }

    static ValidationContextKey newCustomValidationContextKey(String name, ValidationContextKey parent) {
        return new CustomValidationContextKey(name, parent);
    }

    static ValidationContextKey get(String name) {
        return AbstractValidationContextKey.Internal.CACHE.get(name);
    }

    /**
     * Returns a built-in {@link ValidationContextKey.BuiltInValidationContextKey} if present or creates a new {@link ValidationContextKey.CustomValidationContextKey} with the given name and parent.
     * Note that the parent parameter is ignored if a built-in {@link ValidationContextKey} with the given name is found.
     * <p>
     *
     * @param name
     * @param parent
     * @return
     */
    static ValidationContextKey getOrCreate(String name, ValidationContextKey parent) {
        ValidationContextKey key = get(name);
        return key != null ? key : new ValidationContextKey.CustomValidationContextKey(name, parent);
    }

    final class BuiltInValidationContextKey extends AbstractValidationContextKey {

        public BuiltInValidationContextKey(String name, ValidationContextKey parent) {
            super(name, parent);
        }
    }

    final class CustomValidationContextKey extends AbstractValidationContextKey {

        public CustomValidationContextKey(String name, ValidationContextKey parent) {
            super(name, parent);
        }
    }

    final class ActionValidationContextKey implements ValidationActionKey {

        private final ValidationContextKey delegate;

        public ActionValidationContextKey(ValidationContextKey delegate) {
            this.delegate = delegate;
        }

        @Override
        public String getName() {
            return null;
        }
    }

    class AbstractValidationContextKey implements ValidationContextKey {

        /**
         * Lazy static singleton holder for the {@link Internal#CACHE}.
         */
        static class Internal {

            private static final Map<String, ValidationContextKey> CACHE;

            static {
                Map<String, ValidationContextKey> map = new HashMap<>();
                for (ValidationContextKey key : ALL_CONTEXT_KEYS) {
                    map.put(key.getName(), key);
                }
                CACHE = Collections.unmodifiableMap(map);
            }
        }

        private final String name;

        private final ValidationContextKey parent;

        public AbstractValidationContextKey(String name, ValidationContextKey parent) {
            this.name = name;
            this.parent = parent;
        }

        public String getName() {
            return name;
        }

        @Override
        public ValidationContextKey getParent() {
            return parent;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof AbstractValidationContextKey)) {
                return false;
            }
            AbstractValidationContextKey that = (AbstractValidationContextKey) o;
            return Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                    "name='" + name + '\'' +
                    '}';
        }
    }
}
