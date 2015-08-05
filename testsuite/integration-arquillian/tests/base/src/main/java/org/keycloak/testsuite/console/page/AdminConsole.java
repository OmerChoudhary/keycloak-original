/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
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
package org.keycloak.testsuite.console.page;

import java.net.URI;
import org.keycloak.testsuite.auth.page.AuthServer;
import javax.ws.rs.core.UriBuilder;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import static org.keycloak.testsuite.auth.page.AuthRealm.MASTER;
import org.keycloak.testsuite.auth.page.login.PageWithLoginUrl;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author Petr Mensik
 */
public class AdminConsole extends AuthServer implements PageWithLoginUrl {

    public static final String ADMIN_REALM = "adminRealm";

    public AdminConsole() {
        setUriParameter(ADMIN_REALM, MASTER);
    }

    public AdminConsole setAdminRealm(String consoleRealm) {
        setUriParameter(ADMIN_REALM, consoleRealm);
        return this;
    }

    public String getAdminRealm() {
        return getUriParameter(ADMIN_REALM).toString();
    }

    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder().path("admin/{" + ADMIN_REALM + "}/console");
    }

    /**
     *
     * @return OIDC Login URL for adminRealm parameter
     */
    @Override
    public URI getOIDCLoginUrl() {
        return OIDCLoginProtocolService.authUrl(UriBuilder.fromPath(getAuthRoot()))
                .build(getAdminRealm());
    }

    @FindBy(css = ".btn-danger")
    protected WebElement dangerButton;

    //@FindByJQuery(".btn-primary:visible")
    @FindBy(css = ".btn-primary")
    protected WebElement primaryButton;

    @FindBy(css = ".ng-binding.btn.btn-danger")
    protected WebElement deleteConfirmationButton;

    @FindBy(css = "navbar-brand")
    protected WebElement brandLink;

}
