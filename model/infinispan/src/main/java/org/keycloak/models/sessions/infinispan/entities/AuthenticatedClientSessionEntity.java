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

package org.keycloak.models.sessions.infinispan.entities;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.jboss.logging.Logger;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@ProtoTypeId(Marshalling.AUTHENTICATED_CLIENT_SESSION_ENTITY)
public class AuthenticatedClientSessionEntity extends SessionEntity {

    public static final Logger logger = Logger.getLogger(AuthenticatedClientSessionEntity.class);

    // Metadata attribute, which contains the last timestamp available on remoteCache. Used in decide whether we need to write to remoteCache (DC) or not
    public static final String LAST_TIMESTAMP_REMOTE = "lstr";
    public static final String CLIENT_ID_NOTE = "clientId";

    private String authMethod;
    private String redirectUri;
    private volatile int timestamp;
    private String action;

    private Map<String, String> notes = new ConcurrentHashMap<>();

    private String currentRefreshToken;
    private int currentRefreshTokenUseCount;

    private final UUID id;

    private transient String userSessionId;

    public AuthenticatedClientSessionEntity(UUID id) {
        this.id = id;
    }

    @ProtoField(2)
    public String getAuthMethod() {
        return authMethod;
    }

    public void setAuthMethod(String authMethod) {
        this.authMethod = authMethod;
    }

    @ProtoField(3)
    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    @ProtoField(4)
    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public int getUserSessionStarted() {
        String started = getNotes().get(AuthenticatedClientSessionModel.USER_SESSION_STARTED_AT_NOTE);
        return started == null ? timestamp : Integer.parseInt(started);
    }

    public int getStarted() {
        String started = getNotes().get(AuthenticatedClientSessionModel.STARTED_AT_NOTE);
        return started == null ? timestamp : Integer.parseInt(started);
    }

    public boolean isUserSessionRememberMe() {
        return Boolean.parseBoolean(getNotes().get(AuthenticatedClientSessionModel.USER_SESSION_REMEMBER_ME_NOTE));
    }

    public String getClientId() {
        return getNotes().get(CLIENT_ID_NOTE);
    }

    public void setClientId(String clientId) {
        getNotes().put(CLIENT_ID_NOTE, clientId);
    }

    @ProtoField(value = 5)
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    @ProtoField(value = 6, mapImplementation = ConcurrentHashMap.class)
    public Map<String, String> getNotes() {
        return notes;
    }

    public void setNotes(Map<String, String> notes) {
        this.notes = notes;
    }

    @ProtoField(value = 7)
    public String getCurrentRefreshToken() {
        return currentRefreshToken;
    }

    public void setCurrentRefreshToken(String currentRefreshToken) {
        this.currentRefreshToken = currentRefreshToken;
    }

    @ProtoField(8)
    public int getCurrentRefreshTokenUseCount() {
        return currentRefreshTokenUseCount;
    }

    public void setCurrentRefreshTokenUseCount(int currentRefreshTokenUseCount) {
        this.currentRefreshTokenUseCount = currentRefreshTokenUseCount;
    }

    @ProtoField(9)
    public UUID getId() {
        return id;
    }

    @Override
    public String toString() {
        return "AuthenticatedClientSessionEntity [" + "id=" + id + ']';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuthenticatedClientSessionEntity that)) {
            return false;
        }

        return Objects.equals(id, that.id);
    }

    // factory method required because of final fields
    @ProtoFactory
    AuthenticatedClientSessionEntity(String realmId, String authMethod, String redirectUri, int timestamp, String action, Map<String, String> notes, String currentRefreshToken, int currentRefreshTokenUseCount, UUID id) {
        super(realmId);
        this.authMethod = authMethod;
        this.redirectUri = redirectUri;
        this.timestamp = timestamp;
        this.action = action;
        this.notes = notes;
        this.currentRefreshToken = currentRefreshToken;
        this.currentRefreshTokenUseCount = currentRefreshTokenUseCount;
        this.id = id;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public SessionEntityWrapper mergeRemoteEntityWithLocalEntity(SessionEntityWrapper localEntityWrapper) {
        int timestampRemote = getTimestamp();

        SessionEntityWrapper entityWrapper;
        if (localEntityWrapper == null) {
            entityWrapper = new SessionEntityWrapper<>(this);
        } else {
            AuthenticatedClientSessionEntity localClientSession = (AuthenticatedClientSessionEntity) localEntityWrapper.getEntity();

            // local timestamp should always contain the bigger
            if (timestampRemote < localClientSession.getTimestamp()) {
                setTimestamp(localClientSession.getTimestamp());
            }

            entityWrapper = new SessionEntityWrapper<>(localEntityWrapper.getLocalMetadata(), this);
        }

        entityWrapper.putLocalMetadataNoteInt(LAST_TIMESTAMP_REMOTE, timestampRemote);

        logger.debugf("Updating client session entity %s. timestamp=%d, timestampRemote=%d", getId(), getTimestamp(), timestampRemote);

        return entityWrapper;
    }

    public String getUserSessionId() {
        return userSessionId;
    }

    public void setUserSessionId(String userSessionId) {
        this.userSessionId = userSessionId;
    }
}
