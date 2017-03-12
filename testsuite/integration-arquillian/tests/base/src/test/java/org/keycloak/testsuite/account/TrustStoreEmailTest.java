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
package org.keycloak.testsuite.account;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.auth.page.account.AccountManagement;
import org.keycloak.testsuite.auth.page.login.OIDCLogin;
import org.keycloak.testsuite.auth.page.login.VerifyEmail;
import org.keycloak.testsuite.util.MailServerConfiguration;
import org.keycloak.testsuite.util.SslMailServer;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.util.MailAssert.assertEmailAndGetUrl;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;


/**
 *
 * @author fkiss
 */
public class TrustStoreEmailTest extends AbstractTestRealmKeycloakTest {

    @Page
    protected OIDCLogin testRealmLoginPage;

    @Page
    protected AuthRealm testRealmPage;

    @Page
    protected AccountManagement accountManagement;

    @Page
    private VerifyEmail testRealmVerifyEmailPage;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        log.info("enable verify email and configure smtp server to run with ssl in test realm");

        testRealm.setSmtpServer(SslMailServer.getServerConfiguration());
        testRealm.setVerifyEmail(true);
    }


    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmPage.setAuthRealm("test");
        testRealmVerifyEmailPage.setAuthRealm(testRealmPage);
        accountManagement.setAuthRealm(testRealmPage);
        testRealmLoginPage.setAuthRealm(testRealmPage);
    }

    @After
    public void afterTrustStoreEmailTest() {
        SslMailServer.stop();
    }

    @Before
    public void increaseRequestTimeout() {
        driver.manage().timeouts().pageLoadTimeout(30, TimeUnit.SECONDS);
    }

    @Test
    public void verifyEmailWithSslEnabled() {
        UserRepresentation user = ApiUtil.findUserByUsername(testRealm(), "test-user@localhost");

        SslMailServer.startWithSsl(this.getClass().getClassLoader().getResource(SslMailServer.PRIVATE_KEY).getFile());
        accountManagement.navigateTo();
        testRealmLoginPage.form().login(user.getUsername(), "password");

        assertEquals("You need to verify your email address to activate your account.",
                testRealmVerifyEmailPage.getFeedbackText());

        String verifyEmailUrl = assertEmailAndGetUrl(MailServerConfiguration.FROM, user.getEmail(),
                "Someone has created a Test account with this email address.", true);

        log.info("navigating to url from email: " + verifyEmailUrl);

        driver.navigate().to(verifyEmailUrl);

        assertCurrentUrlStartsWith(accountManagement);
        accountManagement.signOut();
        testRealmLoginPage.form().login(user.getUsername(), "password");
        assertCurrentUrlStartsWith(accountManagement);
    }

    @Test
    public void verifyEmailWithSslWrongCertificate() {
        UserRepresentation user = ApiUtil.findUserByUsername(testRealm(), "test-user@localhost");

        SslMailServer.startWithSsl(this.getClass().getClassLoader().getResource(SslMailServer.INVALID_KEY).getFile());
        accountManagement.navigateTo();
        loginPage.form().login(user.getUsername(), "password");

        assertEquals("Failed to send email, please try again later.\n" +
                        "« Back to Application",
                testRealmVerifyEmailPage.getErrorMessage());
    }
}