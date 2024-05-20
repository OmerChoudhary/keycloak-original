/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.cache.infinispan.events;

import java.util.Objects;

import org.infinispan.protostream.annotations.ProtoField;

abstract class BaseRoleEvent extends InvalidationEvent implements RealmCacheInvalidationEvent {

    final String roleId;
    @ProtoField(2)
    final String containerId;

    BaseRoleEvent(String roleId, String containerId) {
        this.roleId = Objects.requireNonNull(roleId);
        this.containerId = Objects.requireNonNull(containerId);
    }

    @Override
    public final String getId() {
        return roleId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        BaseRoleEvent that = (BaseRoleEvent) o;
        return containerId.equals(that.containerId);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + containerId.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s [ roleId=%s, containerId=%s ]", getClass().getSimpleName(), roleId, containerId);
    }
}
