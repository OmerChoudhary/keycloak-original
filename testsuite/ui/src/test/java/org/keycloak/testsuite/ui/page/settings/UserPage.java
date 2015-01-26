package org.keycloak.testsuite.ui.page.settings;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.keycloak.testsuite.ui.model.User;
import org.keycloak.testsuite.ui.page.AbstractPage;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import org.keycloak.testsuite.ui.model.UserAction;
import static org.keycloak.testsuite.ui.util.SeleniumUtils.waitAjaxForElement;
import static org.keycloak.testsuite.ui.util.SeleniumUtils.waitGuiForElement;
import static org.openqa.selenium.By.*;
import org.openqa.selenium.support.ui.Select;

/**
 * Created by fkiss.
 */

public class UserPage extends AbstractPage {

    @FindBy(id = "username")
    private WebElement usernameInput;

    @FindBy(id = "email")
    private WebElement emailInput;

    @FindBy(id = "firstName")
    private WebElement firstNameInput;

    @FindBy(id = "lastName")
    private WebElement lastNameInput;

    @FindBy(id = "emailVerified")
    private WebElement emailVerifiedSwitchToggle;

    @FindBy(css = "label[for='userEnabled']")
    private WebElement userEnabledSwitchToggle;

    @FindBy(css = "input[class*='select2-input']")
    private WebElement requiredUserActionsInput;
	
	@FindByJQuery(".select2-offscreen")
	private Select actionsSelect;
	
	@FindBy(id = "password")
	private WebElement password;
	
	@FindBy(id = "confirmPassword")
	private WebElement confirmPassword;

    @FindBy(css = "input[class*='search']")
    private WebElement searchInput;

    @FindBy(css = "table[class*='table']")
    private WebElement dataTable;

    @FindBy(css = "button.kc-icon-search.ng-scope")
    private WebElement searchButton;

    @FindBy(css = "tr[ng-repeat*='user in users']")
    private WebElement userTable;

    @FindByJQuery("button[kc-cancel] ")
    private WebElement cancel;

    public void addUser(User user) {
        primaryButton.click();
        waitAjaxForElement(usernameInput);
        usernameInput.sendKeys(user.getUserName());
        emailInput.sendKeys(user.getEmail());
		firstNameInput.sendKeys(user.getFirstName());
		lastNameInput.sendKeys(user.getLastName());
        if (!user.isUserEnabled()) {
            userEnabledSwitchToggle.click();
        }
        if (user.isEmailVerified()) {
            emailVerifiedSwitchToggle.click();
        }
        requiredUserActionsInput.sendKeys(user.getRequiredUserActions());
        primaryButton.click();
    }
	
	public void addPasswordForUser(User user) {
		password.sendKeys(user.getPassword());
		confirmPassword.sendKeys(user.getPassword());
		dangerButton.click();
		waitAjaxForElement(deleteConfirmationButton);
		deleteConfirmationButton.click();
	}

    public User findUser(String username) {
        searchInput.sendKeys(username);
        searchButton.click();
        List<User> users = getAllRows();
        if(users.isEmpty()) {
            return null;

        } else {
            assertEquals(1, users.size());
            return users.get(0);
        }
    }

    public void editUser(User user) {
		goToUser(user);
		waitAjaxForElement(usernameInput);
        usernameInput.sendKeys(user.getUserName());
        emailInput.sendKeys(user.getEmail());
        if (!user.isUserEnabled()) {
            userEnabledSwitchToggle.click();
        }
        if (user.isEmailVerified()) {
            emailVerifiedSwitchToggle.click();
        }
        requiredUserActionsInput.sendKeys(user.getRequiredUserActions());
        primaryButton.click();
    }

    public void deleteUser(String username) {
        searchInput.sendKeys(username);
        searchButton.click();
        waitGuiForElement(userTable);
        driver.findElement(linkText(username)).click();
        waitAjaxForElement(dangerButton);
        dangerButton.click();
        waitAjaxForElement(deleteConfirmationButton);
        deleteConfirmationButton.click();
    }

    public void cancel() { 
		cancel.click(); 
	}
	
	public void showAllUsers() {
		driver.findElement(className("kc-link")).click();
	}
	
	public void goToUser(User user) {
		dataTable.findElement(linkText(user.getUserName())).click();
	}
	
	public void goToUser(String name) {
		goToUser(new User(name));
	}
	
	public void addAction(UserAction action) {
		actionsSelect.selectByValue(action.name());
		primaryButton.click();
	}
	
	public void removeAction(UserAction action) {
		actionsSelect.deselectByValue(action.name());
		primaryButton.click();
	}

    private List<User> getAllRows() {
        List<User> rows = new ArrayList<User>();
        for (WebElement rowElement : dataTable.findElements(cssSelector("tbody tr"))) {
            User user = new User();
            List<WebElement> tds = rowElement.findElements(tagName("td"));
            if(!(tds.isEmpty() || tds.get(0).getText().isEmpty())) {
                user.setUserName(tds.get(0).getText());
                user.setLastName(tds.get(1).getText());
                user.setFirstName(tds.get(2).getText());
                user.setEmail(tds.get(3).getText());
                rows.add(user);
            }
        }
        return rows;
    }

}
