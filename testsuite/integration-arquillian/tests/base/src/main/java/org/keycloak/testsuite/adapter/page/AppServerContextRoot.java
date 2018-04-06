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

package org.keycloak.testsuite.adapter.page;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.arquillian.annotation.AppServerContext;
import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;

import java.net.URL;

/**
 *
 * @author tkyjovsk
 */
public class AppServerContextRoot extends AbstractPageWithInjectedUrl {

    @ArquillianResource
    @AppServerContext
    private URL appServerContextRoot;

    @Override
    public URL getInjectedUrl() {
        return appServerContextRoot;
    }

}
