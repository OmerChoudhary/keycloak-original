/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.arquillian;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import static org.hamcrest.Matchers.lessThan;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.spi.Validate;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.After;
import org.jboss.arquillian.test.spi.event.suite.Before;
import org.jboss.logging.Logger;
import static org.junit.Assert.assertThat;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.models.Constants;
import org.keycloak.testsuite.arquillian.annotation.InitialDcState;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.client.KeycloakTestingClient;
import org.keycloak.testsuite.crossdc.DC;
import org.keycloak.testsuite.crossdc.ServerSetup;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.event.StopContainer;
import org.jboss.arquillian.container.spi.event.StopSuiteContainers;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;

/**
 *
 * @author vramik
 */
public class CrossDCTestEnricher {

    protected static final Logger log = Logger.getLogger(CrossDCTestEnricher.class);
    private static SuiteContext suiteContext;

    @Inject
    private static Instance<ContainerController> containerController;

    @Inject
    private Event<StopContainer> stopContainer;

    private static final Map<ContainerInfo, Keycloak> backendAdminClients = new HashMap<>();
    private static final Map<ContainerInfo, KeycloakTestingClient> backendTestingClients = new HashMap<>();

    static void initializeSuiteContext(SuiteContext suiteContext) {
        Validate.notNull(suiteContext, "Suite context cannot be null.");
        CrossDCTestEnricher.suiteContext = suiteContext;
    }

    public void beforeTest(@Observes(precedence = -2) Before event) {
        if (!suiteContext.isAuthServerCrossDc()) return;

        //if annotation is present on method
        InitialDcState annotation = event.getTestMethod().getAnnotation(InitialDcState.class);
        
        //annotation not present on method, taking it from class
        if (annotation == null) {
            Class<?> annotatedClass = getNearestSuperclassWithAnnotation(event.getTestClass().getJavaClass(), InitialDcState.class);

            annotation = annotatedClass == null ? null : annotatedClass.getAnnotation(InitialDcState.class);
        }

        if (annotation == null) {
            log.debug("No environment preparation requested, not changing auth/cache server run status.");
            return; // Test does not specify its environment, so it's on its own
        }

        ServerSetup cacheServers = annotation.cacheServers();
        ServerSetup authServers = annotation.authServers();

        // Stop auth servers that otherwise could be hang connecting to a cache server stopped next
        switch (authServers) {
            case ALL_NODES_IN_EVERY_DC:
                break;
            case FIRST_NODE_IN_EVERY_DC:
                DC.validDcsStream().forEach((DC dc) -> stopAuthServerBackendNode(dc, 1));
                break;

            case FIRST_NODE_IN_FIRST_DC:
                stopAuthServerBackendNode(DC.FIRST, 1);
                forAllBackendNodesInDc(DC.SECOND, CrossDCTestEnricher::stopAuthServerBackendNode);
                break;

            case ALL_NODES_IN_FIRST_DC_FIRST_NODE_IN_SECOND_DC:
                stopAuthServerBackendNode(DC.SECOND, 1);
                break;

            case ALL_NODES_IN_FIRST_DC_NO_NODES_IN_SECOND_DC:
                forAllBackendNodesInDc(DC.SECOND, CrossDCTestEnricher::stopAuthServerBackendNode);
                break;
        }

        switch (cacheServers) {
            case ALL_NODES_IN_EVERY_DC:
            case FIRST_NODE_IN_EVERY_DC: //the same as ALL_NODES_IN_EVERY_DC as there is only one cache server per DC
            case ALL_NODES_IN_FIRST_DC_FIRST_NODE_IN_SECOND_DC:
                DC.validDcsStream().forEach(CrossDCTestEnricher::startCacheServer);
                break;

            case FIRST_NODE_IN_FIRST_DC:
            case ALL_NODES_IN_FIRST_DC_NO_NODES_IN_SECOND_DC:
                startCacheServer(DC.FIRST);
                stopCacheServer(DC.SECOND);
                break;
        }

        switch (authServers) {
            case ALL_NODES_IN_EVERY_DC:
                forAllBackendNodes(CrossDCTestEnricher::startAuthServerBackendNode);
                break;
            case FIRST_NODE_IN_EVERY_DC:
                DC.validDcsStream().forEach((DC dc) -> startAuthServerBackendNode(dc, 0));
                break;

            case FIRST_NODE_IN_FIRST_DC:
                startAuthServerBackendNode(DC.FIRST, 0);
                break;

            case ALL_NODES_IN_FIRST_DC_FIRST_NODE_IN_SECOND_DC:
                forAllBackendNodesInDc(DC.FIRST, CrossDCTestEnricher::startAuthServerBackendNode);
                startAuthServerBackendNode(DC.SECOND, 0);
                break;

            case ALL_NODES_IN_FIRST_DC_NO_NODES_IN_SECOND_DC:
                forAllBackendNodesInDc(DC.FIRST, CrossDCTestEnricher::startAuthServerBackendNode);
                break;
        }
        
        suspendPeriodicTasks();
    }
    
    public void afterTest(@Observes After event) {
        if (!suiteContext.isAuthServerCrossDc()) return;

        restorePeriodicTasks();
    }

    public void afterSuite(@Observes(precedence = 4) AfterSuite event) {
        if (!suiteContext.isAuthServerCrossDc()) return;

        // Unfortunately, in AfterSuite, containerController context is already cleaned so stopAuthServerBackendNode()
        // and stopCacheServer cannot be used. On the other hand, Arquillian by default does not guarantee that cache
        // servers are terminated only after auth servers were, so the termination has to be done in this enricher.

        forAllBackendNodesStream()
          .map(ContainerInfo::getArquillianContainer)
          .map(StopContainer::new)
          .forEach(stopContainer::fire);

        DC.validDcsStream()
          .map(CrossDCTestEnricher::getCacheServer)
          .map(ContainerInfo::getArquillianContainer)
          .map(StopContainer::new)
          .forEach(stopContainer::fire);
    }

    public void stopSuiteContainers(@Observes(precedence = 4) StopSuiteContainers event) {
        if (!suiteContext.isAuthServerCrossDc()) return;

        forAllBackendNodes(CrossDCTestEnricher::stopAuthServerBackendNode);
        DC.validDcsStream().forEach(CrossDCTestEnricher::stopCacheServer);
    }

    private static void createRESTClientsForNode(ContainerInfo node) {
        if (!backendAdminClients.containsKey(node)) {
            backendAdminClients.put(node, createAdminClientFor(node));
        }

        if (!backendTestingClients.containsKey(node)) {
            backendTestingClients.put(node, createTestingClientFor(node));
        }
    }
    
    private static void removeRESTClientsForNode(ContainerInfo node) {
        if (backendAdminClients.containsKey(node)) {
            backendAdminClients.get(node).close();
            backendAdminClients.remove(node);
        }

        if (backendTestingClients.containsKey(node)) {
            backendTestingClients.get(node).close();
            backendTestingClients.remove(node);
        }
    }

    public static Map<ContainerInfo, Keycloak> getBackendAdminClients() {
        return Collections.unmodifiableMap(backendAdminClients);
    }

    public static Map<ContainerInfo, KeycloakTestingClient> getBackendTestingClients() {
        return Collections.unmodifiableMap(backendTestingClients);
    }

    private static Keycloak createAdminClientFor(ContainerInfo node) {
        log.info("--DC: Initializing admin client for " + node.getContextRoot() + "/auth");
        return Keycloak.getInstance(node.getContextRoot() + "/auth", AuthRealm.MASTER, AuthRealm.ADMIN, AuthRealm.ADMIN, Constants.ADMIN_CLI_CLIENT_ID);
    }
    
    private static KeycloakTestingClient createTestingClientFor(ContainerInfo node) {
        log.info("--DC: Initializing testing client for " + node.getContextRoot() + "/auth");
        return KeycloakTestingClient.getInstance(node.getContextRoot() + "/auth");
    }

    // Disable periodic tasks in cross-dc tests. It's needed to have some scenarios more stable.
    private static void suspendPeriodicTasks() {
        log.debug("--DC: suspendPeriodicTasks");
        backendTestingClients.values().stream().forEach((KeycloakTestingClient testingClient) -> {
            testingClient.testing().suspendPeriodicTasks();
        });
    }

    private static void restorePeriodicTasks() {
        log.debug("--DC: restorePeriodicTasks");
        backendTestingClients.values().stream().forEach((KeycloakTestingClient testingClient) -> {
            testingClient.testing().restorePeriodicTasks();
        });
    }

    /**
     * Returns cache server corresponding to given DC
     * @param dc
     * @return
     */
    private static ContainerInfo getCacheServer(DC dc) {
        assertValidDc(dc);
        int dcIndex = dc.ordinal();
        return suiteContext.getCacheServersInfo().get(dcIndex);
    }

    private static void assertValidDc(DC dc) throws IllegalStateException {
        if (dc == DC.UNDEFINED) {
            throw new IllegalStateException("Invalid DC used: " + DC.UNDEFINED);
        }
    }

    public static void startCacheServer(DC dc) {
        if (!containerController.get().isStarted(getCacheServer(dc).getQualifier())) {
            log.infof("--DC: Starting %s", getCacheServer(dc).getQualifier());
            containerController.get().start(getCacheServer(dc).getQualifier());
            log.infof("--DC: Started %s", getCacheServer(dc).getQualifier());
        }
    }

    public static void stopCacheServer(DC dc) {
        String qualifier = getCacheServer(dc).getQualifier();

        if (containerController.get().isStarted(qualifier)) {
            log.infof("--DC: Stopping %s", qualifier);

            containerController.get().stop(qualifier);

            // Workaround for possible arquillian bug. Needs to cleanup dir manually 
            String setupCleanServerBaseDir = getContainerProperty(getCacheServer(dc), "setupCleanServerBaseDir");
            String cleanServerBaseDir = getContainerProperty(getCacheServer(dc), "cleanServerBaseDir");

            if (Boolean.parseBoolean(setupCleanServerBaseDir)) {
                log.debugf("Going to clean directory: %s", cleanServerBaseDir);

                File dir = new File(cleanServerBaseDir);
                if (dir.exists()) {
                    try {
                        dir.renameTo(new File(dir.getParentFile(), dir.getName() + "-backup-" + System.currentTimeMillis()));

                        File deploymentsDir = new File(dir, "deployments");
                        FileUtils.forceMkdir(deploymentsDir);
                    } catch (IOException ioe) {
                        throw new RuntimeException("Failed to clean directory: " + cleanServerBaseDir, ioe);
                    }
                }
            }

            log.infof("--DC: Stopped %s", qualifier);
        }
    }

    public static void forAllBackendNodes(Consumer<ContainerInfo> functionOnContainerInfo) {
        forAllBackendNodesStream()
          .forEach(functionOnContainerInfo);
    }

    public static Stream<ContainerInfo> forAllBackendNodesStream() {
        return suiteContext.getDcAuthServerBackendsInfo().stream()
          .flatMap(Collection::stream);
    }

    public static void forAllBackendNodesInDc(DC dc, Consumer<ContainerInfo> functionOnContainerInfo) {
        assertValidDc(dc);
        suiteContext.getDcAuthServerBackendsInfo().get(dc.ordinal()).stream()
          .forEach(functionOnContainerInfo);
    }

    public static void stopAuthServerBackendNode(ContainerInfo containerInfo) {
        if (containerInfo.isStarted()) {
            log.infof("--DC: Stopping backend auth-server node: %s", containerInfo.getQualifier());
            removeRESTClientsForNode(containerInfo);
            containerController.get().stop(containerInfo.getQualifier());
        }
    }

    public static void startAuthServerBackendNode(ContainerInfo containerInfo) {
        if (! containerInfo.isStarted()) {
            log.infof("--DC: Starting backend auth-server node: %s", containerInfo.getQualifier());
            containerController.get().start(containerInfo.getQualifier());
            createRESTClientsForNode(containerInfo);
        }
    }

    public static ContainerInfo getBackendNode(DC dc, int nodeIndex) {
        assertValidDc(dc);
        int dcIndex = dc.ordinal();
        assertThat((Integer) dcIndex, lessThan(suiteContext.getDcAuthServerBackendsInfo().size()));
        final List<ContainerInfo> dcNodes = suiteContext.getDcAuthServerBackendsInfo().get(dcIndex);
        assertThat((Integer) nodeIndex, lessThan(dcNodes.size()));
        return dcNodes.get(nodeIndex);
    }

    /**
     * Starts a manually-controlled backend auth-server node in cross-DC scenario.
     * @param dc
     * @param nodeIndex
     * @return Started instance descriptor.
     */
    public static ContainerInfo startAuthServerBackendNode(DC dc, int nodeIndex) {
        ContainerInfo dcNode = getBackendNode(dc, nodeIndex);
        startAuthServerBackendNode(dcNode);
        return dcNode;
    }

    /**
     * Stops a manually-controlled backend auth-server node in cross-DC scenario.
     * @param dc
     * @param nodeIndex
     * @return Stopped instance descriptor.
     */
    public static ContainerInfo stopAuthServerBackendNode(DC dc, int nodeIndex) {
        ContainerInfo dcNode = getBackendNode(dc, nodeIndex);
        stopAuthServerBackendNode(dcNode);
        return dcNode;
    }

    private Class getNearestSuperclassWithAnnotation(Class<?> testClass, Class annotationClass) {
        return (testClass.isAnnotationPresent(annotationClass)) ? testClass
                : (testClass.getSuperclass().equals(Object.class) ? null // stop recursion
                : getNearestSuperclassWithAnnotation(testClass.getSuperclass(), annotationClass)); // continue recursion
    }

    private static String getContainerProperty(ContainerInfo cacheServer, String propertyName) {
        return cacheServer.getArquillianContainer().getContainerConfiguration().getContainerProperties().get(propertyName);
    }
}
