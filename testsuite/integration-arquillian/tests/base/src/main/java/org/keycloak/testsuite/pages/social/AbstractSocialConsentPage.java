/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.pages.social;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.logging.Logger;
import org.openqa.selenium.WebDriver;

/**
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public abstract class AbstractSocialConsentPage {
    
    @Drone
    protected WebDriver driver;
    protected Logger log = Logger.getLogger(this.getClass());

    /**
     * Authorize user's consent on the consent page.
     */
    public abstract void authorize();
    
}
