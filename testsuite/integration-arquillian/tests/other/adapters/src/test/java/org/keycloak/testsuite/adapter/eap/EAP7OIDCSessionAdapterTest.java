package org.keycloak.testsuite.adapter.eap;

import org.keycloak.testsuite.adapter.servlet.AbstractSessionServletAdapterTest;
import org.keycloak.testsuite.arquillian.annotation.AdapterLibsLocationProperty;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;

/**
 *
 * @author tkyjovsk
 */
@AppServerContainer("app-server-eap7")
@AdapterLibsLocationProperty("adapter.libs.eap7")
public class EAP7OIDCSessionAdapterTest extends AbstractSessionServletAdapterTest {

}
