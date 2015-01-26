/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.ui.page.settings;

import java.util.ArrayList;
import java.util.List;
import org.jboss.arquillian.graphene.angular.findby.FindByNg;
import static org.junit.Assert.assertNotNull;
import org.keycloak.testsuite.ui.model.Provider;
import org.keycloak.testsuite.ui.model.SocialProvider;
import org.keycloak.testsuite.ui.page.AbstractPage;
import static org.openqa.selenium.By.tagName;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 *
 * @author pmensik
 */
public class SocialSettingsPage extends AbstractPage {
	
	@FindByNg(model = "newProviderId")
	private Select newProviderSelect;
	
	@FindByNg(model = "realm.socialProviders[pId+'.key']")
	private WebElement providerKey;
	
	@FindByNg(model = "realm.socialProviders[pId+'.secret']")
	private WebElement providerSecret;
	
	@FindBy(tagName = "tbody")
	private WebElement providersTable;
	
	public void addNewProvider(Provider provider) {
		newProviderSelect.selectByVisibleText(provider.providerName.getName());
		providerKey.sendKeys(provider.key);
		providerSecret.sendKeys(provider.secret);
		primaryButton.click();
	}
	
	public void editProvider(SocialProvider oldProvider, Provider newProvider) {
		Provider p = find(oldProvider);
		assertNotNull("Provider should have been found", p);
		System.out.println(p.providerName);
	}
	
	public Provider find(SocialProvider provider){
		List<Provider> list = getAllRows();
		for(Provider p : list) {
			if(p.providerName == provider) {
				return p;
			} 
		}
		return null;
	}
	
	private List<Provider> getAllRows() {
        List<Provider> rows = new ArrayList<Provider>();
        for (WebElement rowElement : providersTable.findElements(tagName("tr"))) {
            Provider provider = new Provider();
            List<WebElement> tds = rowElement.findElements(tagName("td"));
            if(!(tds.isEmpty() || tds.get(0).getText().isEmpty())) {
                provider.providerName = SocialProvider.valueOf(tds.get(0).getText());
                provider.key = tds.get(1).getText();
                provider.secret = tds.get(2).getText();
                rows.add(provider);
            }
        }
        return rows;
    }
}
