/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.testsuite.federation.storage;

import jakarta.ws.rs.core.Response;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.Profile.Feature;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.federation.BackwardsCompatibilityUserStorageFactory;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginConfigTotpPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginTotpPage;
import org.keycloak.testsuite.util.TestAppHelper;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.wildfly.common.Assert.assertTrue;

/**
 * Test that userStorage implementation created in previous version is still compatible with latest Keycloak version
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class BackwardsCompatibilityUserStorageTest extends AbstractTestRealmKeycloakTest {

    private String backwardsCompProviderId;

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected LoginTotpPage loginTotpPage;

    @Page
    protected LoginConfigTotpPage configureTotpRequiredActionPage;


    private TimeBasedOTP totp = new TimeBasedOTP();



    @BeforeClass
    public static void checkNotMapStorage() {
        ProfileAssume.assumeFeatureDisabled(Feature.MAP_STORAGE);
    }

    @Before
    public void addProvidersBeforeTest() throws URISyntaxException, IOException {
        ComponentRepresentation memProvider = new ComponentRepresentation();
        memProvider.setName("backwards-compatibility");
        memProvider.setProviderId(BackwardsCompatibilityUserStorageFactory.PROVIDER_ID);
        memProvider.setProviderType(UserStorageProvider.class.getName());
        memProvider.setConfig(new MultivaluedHashMap<>());
        memProvider.getConfig().putSingle("priority", Integer.toString(0));

        backwardsCompProviderId = addComponent(memProvider);

    }

    protected String addComponent(ComponentRepresentation component) {
        Response resp = testRealm().components().add(component);
        String id = ApiUtil.getCreatedId(resp);
        getCleanup().addComponentId(id);
        return id;
    }

    private void loginSuccessAndLogout(String username, String password) throws URISyntaxException, IOException {
        TestAppHelper testAppHelper = new TestAppHelper(oauth, loginPage, appPage);

        testAppHelper.login(username, password);
        appPage.assertCurrent();

        assertTrue(testAppHelper.logout());
    }

    public void loginBadPassword(String username) {
        loginPage.open();
        loginPage.login(username, "badpassword");
        loginPage.assertCurrent();
    }

    @Test
    public void testLoginSuccess() throws URISyntaxException, IOException {
        addUserAndResetPassword("tbrady", "goat");
        addUserAndResetPassword("tbrady2", "goat2");

        loginSuccessAndLogout("tbrady", "goat");
        loginSuccessAndLogout("tbrady2", "goat2");
        loginBadPassword("tbrady");
    }

    private String addUserAndResetPassword(String username, String password) {
        // Save user and assert he is saved in the new storage
        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername(username);
        Response response = testRealm().users().create(user);
        String userId = ApiUtil.getCreatedId(response);

        Assert.assertEquals(backwardsCompProviderId, new StorageId(userId).getProviderId());

        // Update his password
        CredentialRepresentation passwordRep = new CredentialRepresentation();
        passwordRep.setType(CredentialModel.PASSWORD);
        passwordRep.setValue(password);
        passwordRep.setTemporary(false);

        testRealm().users().get(userId).resetPassword(passwordRep);

        return userId;
    }


    @Test
    public void testOTPUpdateAndLogin() throws URISyntaxException, IOException {
        String userId = addUserAndResetPassword("otp1", "pass");
        getCleanup().addUserId(userId);

        // Setup OTP for the user
        String totpSecret = setupOTPForUserWithRequiredAction(userId);

        // Assert user has OTP in the userStorage
        assertUserDontHaveDBCredentials();
        assertUserHasOTPCredentialInUserStorage(true);

        assertUserDontHaveDBCredentials();
        assertUserHasOTPCredentialInUserStorage(true);

        TestAppHelper testAppHelper = new TestAppHelper(oauth, loginPage, appPage);

        // Authenticate as the user with the hardcoded OTP. Should be supported
        testAppHelper.startLogin("otp1", "pass");
        loginTotpPage.login("123456");
        testAppHelper.completeLogin();

        appPage.assertCurrent();

        testAppHelper.logout();

        // Authenticate as the user with bad OTP
        testAppHelper.startLogin("otp1", "pass");
        loginTotpPage.assertCurrent();
        loginTotpPage.login("7123456");
        loginTotpPage.assertCurrent();
        Assert.assertNotNull(loginTotpPage.getInputError());

        // Authenticate as the user with correct OTP
        loginTotpPage.login(totp.generateTOTP(totpSecret));
        testAppHelper.completeLogin();
        appPage.assertCurrent();

        assertTrue(testAppHelper.logout());
    }

    @Test
    public void testOTPSetupThroughAccountMgmtAndLogin() throws URISyntaxException, IOException {
        String userId = addUserAndResetPassword("otp1", "pass");
        getCleanup().addUserId(userId);

        // Setup OTP
        String totpSecret = setupOTPForUserWithRequiredAction(userId);

        assertUserDontHaveDBCredentials();
        assertUserHasOTPCredentialInUserStorage(true);

        TestAppHelper testAppHelper = new TestAppHelper(oauth, loginPage, loginTotpPage, appPage);

        // Login as user to account mgmt
        assertTrue(testAppHelper.login("otp1", "pass", "123456"));

        // Logout and assert user can login with valid credential
        testAppHelper.logout();
        assertTrue(testAppHelper.login("otp1", "pass", totp.generateTOTP(totpSecret)));
        testAppHelper.logout();

        // Disable OTP credential in account console
        testRealm().users().get(userId).disableCredentialType(Collections.singletonList(OTPCredentialModel.TYPE));

        assertUserDontHaveDBCredentials();
        assertUserHasOTPCredentialInUserStorage(false);

        // Assert user can login without OTP
        loginSuccessAndLogout("otp1", "pass");
    }

    @Test
    public void testDisableCredentialsInUserStorage() throws URISyntaxException, IOException {
        String userId = addUserAndResetPassword("otp1", "pass");
        getCleanup().addUserId(userId);

        // Setup OTP for the user
        setupOTPForUserWithRequiredAction(userId);

        // Assert user has OTP in the userStorage
        assertUserDontHaveDBCredentials();
        assertUserHasOTPCredentialInUserStorage(true);

        UserResource user = testRealm().users().get(userId);

        // Disable OTP credential for the user through REST endpoint
        UserRepresentation userRep = user.toRepresentation();
        Assert.assertNames(userRep.getDisableableCredentialTypes(), OTPCredentialModel.TYPE);

        user.disableCredentialType(Collections.singletonList(OTPCredentialModel.TYPE));

        // User don't have OTP credential in userStorage anymore
        assertUserDontHaveDBCredentials();
        assertUserHasOTPCredentialInUserStorage(false);

        // Assert user can login without OTP
        loginSuccessAndLogout("otp1", "pass");
    }


    @Test
    public void testSearchUserStorage() {
        String userId = addUserAndResetPassword("searching", "pass");
        getCleanup().addUserId(userId);

        // Uses same parameters as admin console when searching users
        List<UserRepresentation> users = testRealm().users().search("searching", 0, 20, true);
        Assert.assertNames(users, "searching");
    }

    // return created totpSecret
    private String setupOTPForUserWithRequiredAction(String userId) throws URISyntaxException, IOException {
        // Add required action to the user to reset OTP
        UserResource user = testRealm().users().get(userId);
        UserRepresentation userRep = user.toRepresentation();
        userRep.setRequiredActions(Arrays.asList(UserModel.RequiredAction.CONFIGURE_TOTP.toString()));
        user.update(userRep);

        TestAppHelper testAppHelper = new TestAppHelper(oauth, loginPage, appPage);

        // Login as the user and setup OTP
        testAppHelper.startLogin("otp1", "pass");

        configureTotpRequiredActionPage.assertCurrent();
        String totpSecret = configureTotpRequiredActionPage.getTotpSecret();
        configureTotpRequiredActionPage.configure(totp.generateTOTP(totpSecret));
        appPage.assertCurrent();

        testAppHelper.completeLogin();

        // Logout
        assertTrue(testAppHelper.logout());

        return totpSecret;
    }


    private void assertUserDontHaveDBCredentials() {
        testingClient.server().run(session -> {
            RealmModel realm1 = session.realms().getRealmByName("test");
            UserModel user1 = session.users().getUserByUsername(realm1, "otp1");
            Assert.assertEquals(0, user1.credentialManager().getStoredCredentialsStream().count());
        });
    }

    private void assertUserHasOTPCredentialInUserStorage(boolean expectedUserHasOTP) {
        boolean hasUserOTP = testingClient.server().fetch(session -> {
            BackwardsCompatibilityUserStorageFactory storageFactory = (BackwardsCompatibilityUserStorageFactory) session.getKeycloakSessionFactory()
                    .getProviderFactory(UserStorageProvider.class, BackwardsCompatibilityUserStorageFactory.PROVIDER_ID);
            return storageFactory.hasUserOTP("otp1");
        }, Boolean.class);
        Assert.assertEquals(expectedUserHasOTP, hasUserOTP);
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {

    }
}
