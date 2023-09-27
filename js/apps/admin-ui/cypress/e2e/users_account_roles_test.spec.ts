import LoginPage from "../support/pages/LoginPage";
import Masthead from "../support/pages/admin-ui/Masthead";
import SidebarPage from "../support/pages/admin-ui/SidebarPage";
import { keycloakBefore } from "../support/util/keycloak_hooks";
import CreateUserPage from "../support/pages/admin-ui/manage/users/CreateUserPage";
import CredentialsPage from "../support/pages/admin-ui/manage/users/CredentialsPage";
import RoleMappingTab from "../support/pages/admin-ui/manage/RoleMappingTab";
import CreateProviderPage from "../support/pages/admin-ui/manage/identity_providers/CreateProviderPage";
import RequiredActions from "../support/pages/admin-ui/manage/authentication/RequiredActions";
import adminClient from "../support/util/AdminClient";

const loginPage = new LoginPage();
const masthead = new Masthead();
const sidebarPage = new SidebarPage();
const createUserPage = new CreateUserPage();
const credentialsPage = new CredentialsPage();
const roleMappingTab = new RoleMappingTab("");
const createProviderPage = new CreateProviderPage();
const requiredActionsPage = new RequiredActions();

const itemId = "test";

describe("User account roles tests", () => {
  beforeEach(() => {
    loginPage.logIn();
    keycloakBefore();
    sidebarPage.goToUsers();

    createUserPage.goToCreateUser();
    createUserPage.createUser(itemId);
    createUserPage.save();
    masthead.checkNotificationMessage("The user has been created");
    sidebarPage.waitForPageLoad();

    credentialsPage
      .goToCredentialsTab()
      .clickEmptyStatePasswordBtn()
      .fillPasswordFormWithTempOff()
      .clickConfirmationBtn()
      .clickSetPasswordBtn();
  });

  afterEach(() => {
    adminClient.deleteUser(itemId);
  });

  it("should check that user with inherited roles (view-profile, manage-account-links, manage-account) can access and perform specific actions in account console", () => {
    //Add identity provider
    sidebarPage.goToIdentityProviders();
    createProviderPage
      .clickItem("bitbucket-card")
      .fill("bitbucket", "123")
      .clickAdd();

    //Sign out and login as created user
    masthead.signOut();
    loginPage.logIn("test", "test");
    keycloakBefore();

    masthead.accountManagement();
    sidebarPage.waitForPageLoad();

    //Check that user can view personal info
    cy.findByTestId("username").should("have.value", "test");

    //Check that user can update email in personal info
    cy.findByTestId("email").type("test@test.com");
    cy.findByTestId("firstName").type("testFirstName");
    cy.findByTestId("lastName").type("testLastName");
    cy.findByTestId("save").click();
    cy.get(
      'h4.pf-c-alert__title:contains("Your account has been updated.")',
    ).should("exist");

    //Check that user doesn't have access to delete account from personal info
    cy.contains("Delete account").should("not.exist");

    //Check that user can access linked accounts under account security
    cy.contains("Account security").click();
    cy.contains("Linked accounts").click();
    cy.contains("Link account").should("exist");

    //Check that user doesn't have access to groups
    cy.contains("Groups").should("not.exist");

    // Todo: Clean up
    // masthead.signOut();
    // loginPage.logIn("admin", "admin");

    // sidebarPage.goToIdentityProviders();
    // delete bitbucket identity provider
  });

  it("should check that user with delete-account role has an access to delete account in account console", () => {
    roleMappingTab.goToRoleMappingTab();
    cy.findByTestId("assignRole").click({ force: true });
    cy.findByTestId("filter-type-dropdown").click();
    cy.findByTestId("roles").click();
    cy.get('input[placeholder="Search by role name"]').type(
      "delete-account{enter}",
    );
    roleMappingTab.selectRow("delete-account", true).assign();

    // Enable delete account in the Authentication/ Required action
    sidebarPage.goToAuthentication();

    const action = "Delete Account";
    requiredActionsPage.enableAction(action);
    masthead.checkNotificationMessage("Updated required action successfully");
    requiredActionsPage.isChecked(action);

    //Sign out and login as created user
    masthead.signOut();
    loginPage.logIn("test", "test");
    keycloakBefore();

    masthead.accountManagement();
    sidebarPage.waitForPageLoad();

    //Check that user has access to delete account from personal info
    cy.contains("Delete account").should("exist");

    // Todo: Clean up
    // masthead.signOut();
    // loginPage.logIn("admin", "admin");

    // sidebarPage.goToAuthentication();
    // set delete account to off
  });

  it("should check that user with view-groups role has an access to groups in account console", () => {
    roleMappingTab.goToRoleMappingTab();
    cy.findByTestId("assignRole").click({ force: true });
    cy.findByTestId("filter-type-dropdown").click();
    cy.findByTestId("roles").click();
    cy.get('input[placeholder="Search by role name"]').type(
      "view-groups{enter}",
    );
    roleMappingTab.selectRow("view-groups", true).assign();

    //Sign out and login as created user
    masthead.signOut();
    loginPage.logIn("test", "test");
    keycloakBefore();

    masthead.accountManagement();
    sidebarPage.waitForPageLoad();

    //Check that user has access to delete account from personal info
    cy.contains("Groups").should("exist").click();
    cy.get('h1.pf-c-title.pf-m-2xl:contains("Groups")').should("exist");
  });
});
