package org.keycloak.testsuite;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.RealmRepresentation;
import static org.keycloak.testsuite.util.RealmUtils.importRealm;
import static org.keycloak.testsuite.util.RealmUtils.removeRealm;
import org.openqa.selenium.WebDriver;
import org.keycloak.testsuite.arquillian.ContainersTestEnricher.AdminPasswordUpdateTracker;
import org.keycloak.testsuite.console.page.AdminConsole;
import org.keycloak.testsuite.auth.page.AuthServer;
import org.keycloak.testsuite.auth.page.AuthServerContextRoot;
import static org.keycloak.testsuite.util.PageAssert.*;
import org.keycloak.testsuite.console.page.fragment.Menu;
import org.keycloak.testsuite.auth.page.login.Login;
import org.keycloak.testsuite.auth.page.account.Password;
import org.keycloak.testsuite.auth.page.AuthRealm;
import static org.keycloak.testsuite.auth.page.AuthRealm.ADMIN;
import static org.keycloak.testsuite.auth.page.AuthRealm.MASTER;
import static org.keycloak.testsuite.util.Constants.ADMIN_PSSWD;
import static org.keycloak.testsuite.util.LoginAssert.assertCurrentUrlStartsWithLoginUrlOf;
import static org.keycloak.testsuite.util.SeleniumUtils.pause;

/**
 *
 * @author tkyjovsk
 */
@RunWith(Arquillian.class)
@RunAsClient
public abstract class AbstractKeycloakTest {

    protected Keycloak keycloak;
    protected boolean keycloakOpen = false;

    protected List<RealmRepresentation> testRealms;

    @Drone
    protected WebDriver driver;

    @Page
    protected AuthServerContextRoot authServerContextRoot;
    @Page
    protected AuthServer authServer;

    @Page
    protected AuthRealm masterAuthRealm;
    @Page
    protected AdminConsole masterAdminConsole;

    @Page
    protected Login masterLogin;
    @Page
    protected Password password;
    @Page
    protected Menu menu;

    @Before
    public void beforeAbstractKeycloakTest() {
        setDefaultPageUriParameters();

        driverSettings();

        if (!isAdminPasswordUpdated()) {
            updateMasterAdminPassword();
        }

        if (!keycloakOpen) {
            keycloak = Keycloak.getInstance(authServer.toString(),
                    MASTER, ADMIN, ADMIN, Constants.ADMIN_CONSOLE_CLIENT_ID);
            keycloakOpen = true;
        }
        
        pause(1000);

        importTestRealms();
    }

    @After
    public void afterAbstractKeycloakTest() {
//        removeTestRealms();
//        keycloak.close();
    }

    public boolean isAdminPasswordUpdated() {
        return AdminPasswordUpdateTracker.isAdminPasswordUpdated(this.getClass());
    }

    public void setAdminPasswordUpdated(boolean updated) {
        AdminPasswordUpdateTracker
                .setAdminPasswordUpdatedFor(this.getClass(), updated);
    }

    private void updateMasterAdminPassword() {
        loginToMasterRealmConsoleAsAdmin();
        password.confirmNewPassword(ADMIN_PSSWD);
        password.submit();
        assertCurrentUrlStartsWith(masterAdminConsole);
        setAdminPasswordUpdated(true);
        logoutFromMasterRealmConsole();
    }

    public void loginToMasterRealmConsoleAsAdmin() {
        masterAdminConsole.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(masterAdminConsole);
        masterLogin.loginAsAdmin();
        if (isAdminPasswordUpdated()) {
            assertCurrentUrlStartsWith(masterAdminConsole);
        }
    }

    public void logoutFromMasterRealmConsole() {
        masterAdminConsole.navigateTo();
        assertCurrentUrlStartsWith(masterAdminConsole);
        menu.logOut();
    }

    protected void driverSettings() {
        masterAuthRealm.navigateTo(); // navigate to /auth/realms/master before deleting cookies
        driver.manage().deleteAllCookies();
        driver.manage().timeouts().setScriptTimeout(10, TimeUnit.SECONDS);
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        driver.manage().timeouts().setScriptTimeout(10, TimeUnit.SECONDS);
        driver.manage().window().maximize();
    }

    public void setDefaultPageUriParameters() {
        masterAuthRealm.setAuthRealm(MASTER);
        masterLogin.setAuthRealm(MASTER);
        masterAdminConsole.setAdminRealm(MASTER);
    }

    public abstract void addTestRealms(List<RealmRepresentation> testRealms);

    private void addTestRealms() {
        System.out.println("loading test realms");
        if (testRealms == null) {
            testRealms = new ArrayList<>();
        }
        if (testRealms.isEmpty()) {
            addTestRealms(testRealms);
        }
    }

    public void importTestRealms() {
        addTestRealms();
        System.out.println("importing test realms");
        for (RealmRepresentation testRealm : testRealms) {
            importRealm(keycloak, testRealm);
        }
    }

    public void removeTestRealms() {
        System.out.println("removing test realms");
        for (RealmRepresentation testRealm : testRealms) {
            removeRealm(keycloak, testRealm);
        }
    }

}
