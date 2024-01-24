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

package org.keycloak.protocol.oidc.grants;

import jakarta.ws.rs.core.Response;

import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.Profile;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.events.Errors;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.utils.OAuth2Code;
import org.keycloak.protocol.oidc.utils.OAuth2CodeParser;
import org.keycloak.protocol.oidc.utils.PkceUtils;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.TokenRequestContext;
import org.keycloak.services.clientpolicy.context.TokenResponseContext;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.util.DefaultClientSessionContext;

/**
 * OAuth 2.0 Authorization Code Grant
 * https://datatracker.ietf.org/doc/html/rfc6749#section-4.1
 *
 * @author <a href="mailto:demetrio@carretti.pro">Dmitry Telegin</a> (et al.)
 */
public class AuthorizationCodeGrantType extends OAuth2GrantTypeBase {

    private static final Logger logger = Logger.getLogger(AuthorizationCodeGrantType.class);
    private static final String PROVIDER_ID = "authorization_code";

    @Override
    public Response process() {
        checkAndRetrieveDPoPProof(Profile.isFeatureEnabled(Profile.Feature.DPOP));

        String code = formParams.getFirst(OAuth2Constants.CODE);
        if (code == null) {
            event.error(Errors.INVALID_CODE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Missing parameter: " + OAuth2Constants.CODE, Response.Status.BAD_REQUEST);
        }

        OAuth2CodeParser.ParseResult parseResult = OAuth2CodeParser.parseCode(session, code, realm, event);
        if (parseResult.isIllegalCode()) {
            AuthenticatedClientSessionModel clientSession = parseResult.getClientSession();

            // Attempt to use same code twice should invalidate existing clientSession
            if (clientSession != null) {
                clientSession.detachFromUserSession();
            }

            event.error(Errors.INVALID_CODE);

            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Code not valid", Response.Status.BAD_REQUEST);
        }

        AuthenticatedClientSessionModel clientSession = parseResult.getClientSession();

        if (parseResult.isExpiredCode()) {
            event.error(Errors.EXPIRED_CODE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Code is expired", Response.Status.BAD_REQUEST);
        }

        UserSessionModel userSession = null;
        if (clientSession != null) {
            userSession = clientSession.getUserSession();
        }

        if (userSession == null) {
            event.error(Errors.USER_SESSION_NOT_FOUND);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "User session not found", Response.Status.BAD_REQUEST);
        }


        UserModel user = userSession.getUser();
        if (user == null) {
            event.error(Errors.USER_NOT_FOUND);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "User not found", Response.Status.BAD_REQUEST);
        }

        event.user(userSession.getUser());

        if (!user.isEnabled()) {
            event.error(Errors.USER_DISABLED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "User disabled", Response.Status.BAD_REQUEST);
        }

        OAuth2Code codeData = parseResult.getCodeData();
        String redirectUri = codeData.getRedirectUriParam();
        String redirectUriParam = formParams.getFirst(OAuth2Constants.REDIRECT_URI);

        // KEYCLOAK-4478 Backwards compatibility with the adapters earlier than KC 3.4.2
        if (redirectUriParam != null && redirectUriParam.contains("session_state=") && !redirectUri.contains("session_state=")) {
            redirectUriParam = KeycloakUriBuilder.fromUri(redirectUriParam)
                    .replaceQueryParam(OAuth2Constants.SESSION_STATE, null)
                    .build().toString();
        }

        if (redirectUri != null && !redirectUri.equals(redirectUriParam)) {
            event.error(Errors.INVALID_CODE);
            logger.tracef("Parameter 'redirect_uri' did not match originally saved redirect URI used in initial OIDC request. Saved redirectUri: %s, redirectUri parameter: %s", redirectUri, redirectUriParam);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Incorrect redirect_uri", Response.Status.BAD_REQUEST);
        }

        if (!client.getClientId().equals(clientSession.getClient().getClientId())) {
            event.error(Errors.INVALID_CODE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Auth error", Response.Status.BAD_REQUEST);
        }

        if (!client.isStandardFlowEnabled()) {
            event.error(Errors.NOT_ALLOWED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Client not allowed to exchange code", Response.Status.BAD_REQUEST);
        }

        if (!AuthenticationManager.isSessionValid(realm, userSession)) {
            event.error(Errors.USER_SESSION_NOT_FOUND);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Session not active", Response.Status.BAD_REQUEST);
        }

        // https://tools.ietf.org/html/rfc7636#section-4.6
        String codeVerifier = formParams.getFirst(OAuth2Constants.CODE_VERIFIER);
        String codeChallenge = codeData.getCodeChallenge();
        String codeChallengeMethod = codeData.getCodeChallengeMethod();
        String authUserId = user.getId();
        String authUsername = user.getUsername();
        if (authUserId == null) {
            authUserId = "unknown";
        }
        if (authUsername == null) {
            authUsername = "unknown";
        }

        if (codeChallengeMethod != null && !codeChallengeMethod.isEmpty()) {
            PkceUtils.checkParamsForPkceEnforcedClient(codeVerifier, codeChallenge, codeChallengeMethod, authUserId, authUsername, event, cors);
        } else {
            // PKCE Activation is OFF, execute the codes implemented in KEYCLOAK-2604
            PkceUtils.checkParamsForPkceNotEnforcedClient(codeVerifier, codeChallenge, codeChallengeMethod, authUserId, authUsername, event, cors);
        }

        try {
            session.clientPolicy().triggerOnEvent(new TokenRequestContext(formParams, parseResult));
        } catch (ClientPolicyException cpe) {
            event.error(cpe.getError());
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, cpe.getErrorDetail(), Response.Status.BAD_REQUEST);
        }

        updateClientSession(clientSession);
        updateUserSessionFromClientAuth(userSession);

        // Compute client scopes again from scope parameter. Check if user still has them granted
        // (but in code-to-token request, it could just theoretically happen that they are not available)
        String scopeParam = codeData.getScope();
        Supplier<Stream<ClientScopeModel>> clientScopesSupplier = () -> TokenManager.getRequestedClientScopes(scopeParam, client);
        if (!TokenManager.verifyConsentStillAvailable(session, user, client, clientScopesSupplier.get())) {
            event.error(Errors.NOT_ALLOWED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_SCOPE, "Client no longer has requested consent from user", Response.Status.BAD_REQUEST);
        }

        ClientSessionContext clientSessionCtx = DefaultClientSessionContext.fromClientSessionAndScopeParameter(clientSession, scopeParam, session);

        // Set nonce as an attribute in the ClientSessionContext. Will be used for the token generation
        clientSessionCtx.setAttribute(OIDCLoginProtocol.NONCE_PARAM, codeData.getNonce());

        return createTokenResponse(user, userSession, clientSessionCtx, scopeParam, true, s -> {return new TokenResponseContext(formParams, parseResult, clientSessionCtx, s);});
    }

    @Override
    public OAuth2GrantType create(KeycloakSession session) {
        return new AuthorizationCodeGrantType();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getGrantType() {
        return OAuth2Constants.AUTHORIZATION_CODE;
    }

}
