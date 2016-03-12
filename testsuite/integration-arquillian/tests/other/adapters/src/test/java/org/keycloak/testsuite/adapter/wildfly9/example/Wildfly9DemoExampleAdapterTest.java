package org.keycloak.testsuite.adapter.wildfly9.example;

import org.keycloak.testsuite.adapter.example.AbstractDemoExampleAdapterTest;
import org.keycloak.testsuite.arquillian.annotation.AdapterLibsLocationProperty;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;

/**
 *
 * @author tkyjovsk
 */
@AppServerContainer("app-server-wildfly")
@AdapterLibsLocationProperty("adapter.libs.wildfly")
public class Wildfly9DemoExampleAdapterTest extends AbstractDemoExampleAdapterTest {

}