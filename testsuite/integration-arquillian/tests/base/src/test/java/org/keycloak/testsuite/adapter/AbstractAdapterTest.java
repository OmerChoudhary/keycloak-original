package org.keycloak.testsuite.adapter;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.arquillian.ContainersTestEnricher;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.adapter.page.AppServerContextRoot;
import org.keycloak.testsuite.page.auth.AuthRealm;

/**
 *
 * @author tkyjovsk
 */
@AppServerContainer
public abstract class AbstractAdapterTest extends AbstractKeycloakTest {

    @Page
    protected AppServerContextRoot appServerContextRoot;

    public static final String JBOSS_DEPLOYMENT_STRUCTURE_XML = "jboss-deployment-structure.xml";
    public static final URL jbossDeploymentStructure = AbstractServletsAdapterTest.class
            .getResource("/adapter-test/" + JBOSS_DEPLOYMENT_STRUCTURE_XML);
    public static final String TOMCAT_CONTEXT_XML = "context.xml";
    public static final URL tomcatContext = AbstractServletsAdapterTest.class
            .getResource("/adapter-test/" + TOMCAT_CONTEXT_XML);

    @Page
    protected AuthRealm authRealm;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        addAdapterTestRealms(testRealms);
        for (RealmRepresentation tr : testRealms) {
            System.out.println("Setting redirect-uris in test realm '" + tr.getRealm() + "' as " + (isRelative() ? "" : "non-") + "relative");
            if (isRelative()) {
                modifyClientRedirectUris(tr, appServerContextRoot.toString(), "");
                modifyClientUrls(tr, appServerContextRoot.toString(), "");
            } else {
                modifyClientRedirectUris(tr, "^(/.*/\\*)", appServerContextRoot.toString() + "$1");
                modifyClientUrls(tr, "^(/.*)", appServerContextRoot.toString() + "$1");
            }
        }
    }

    public abstract void addAdapterTestRealms(List<RealmRepresentation> testRealms);

    public boolean isRelative() {
        return ContainersTestEnricher.isRelative(this.getClass());
    }

    protected void modifyClientRedirectUris(RealmRepresentation realm, String regex, String replacement) {
        for (ClientRepresentation client : realm.getClients()) {
            List<String> redirectUris = client.getRedirectUris();
            if (redirectUris != null) {
                List<String> newRedirectUris = new ArrayList<>();
                for (String uri : redirectUris) {
                    newRedirectUris.add(uri.replaceAll(regex, replacement));
                }
                client.setRedirectUris(newRedirectUris);
            }
        }
    }

    protected void modifyClientUrls(RealmRepresentation realm, String regex, String replacement) {
        for (ClientRepresentation client : realm.getClients()) {
            String baseUrl = client.getBaseUrl();
            if (baseUrl != null) {
                client.setBaseUrl(baseUrl.replaceAll(regex, replacement));
            }
            String adminUrl = client.getAdminUrl();
            if (adminUrl != null) {
                client.setAdminUrl(adminUrl.replaceAll(regex, replacement));
            }
        }
    }

    /**
     * Modifies baseUrl, adminUrl and redirectUris for client based on real
     * deployment url of the app.
     *
     * @param realm
     * @param clientId
     * @param deploymentUrl
     */
    protected void fixClientUrisUsingDeploymentUrl(RealmRepresentation realm, String clientId, String deploymentUrl) {
        for (ClientRepresentation client : realm.getClients()) {
            if (clientId.equals(client.getClientId())) {
                if (client.getBaseUrl() != null) {
                    client.setBaseUrl(deploymentUrl);
                }
                if (client.getAdminUrl() != null) {
                    client.setAdminUrl(deploymentUrl);
                }
                List<String> redirectUris = client.getRedirectUris();
                if (redirectUris != null) {
                    List<String> newRedirectUris = new ArrayList<>();
                    for (String uri : redirectUris) {
                        newRedirectUris.add(deploymentUrl + "/*");
                    }
                    client.setRedirectUris(newRedirectUris);
                }
            }
        }
    }

    public static void addContextXml(Archive archive, String contextPath) {
        try {
            String contextXmlContent = IOUtils.toString(tomcatContext.openStream())
                    .replace("%CONTEXT_PATH%", contextPath);
            archive.add(new StringAsset(contextXmlContent), "/META-INF/context.xml");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
