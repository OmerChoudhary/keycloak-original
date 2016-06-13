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

package org.keycloak.connections.jpa.entityprovider;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:erik.mulder@docdatapayments.com">Erik Mulder</a>
 * 
 * Classloader implementation to facilitate loading extra entity classes.
 * The way it works:
 * - Get all (unique) classloaders from all provided entity classes
 * - For each class that is 'requested':
 *   - First try all provided classloaders and if we have a match, return that
 *   - If no match was found: proceed with 'normal' classloading in 'current classpath' scope
 * 
 * In this particular context: only loadClass and getResource overrides are needed, since those
 * are the methods that the Hibernate classloading process will call during entity manager initialization.
 */
public class ProvidedEntitiesClassLoader extends ClassLoader {

    private Set<ClassLoader> classloaders;

    public ProvidedEntitiesClassLoader(List<Class<?>> providedEntities, ClassLoader parentClassLoader) {
    	super(parentClassLoader);
        classloaders = new HashSet<>();
        for (Class<?> clazz : providedEntities) {
            classloaders.add(clazz.getClassLoader());
        }
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        for (ClassLoader classloader : classloaders) {
            try {
                return classloader.loadClass(name);
            } catch (ClassNotFoundException e) {
                // This particular class loader did not find the class. It's expected behavior that
                // this can happen, so we'll just ignore the exception and let the next one try.
            }
        }
        // We did not find the class in the provided entity class loaders, so proceed with 'normal' behavior.
        return super.loadClass(name);
    }

    @Override
    public URL getResource(String name) {
        for (ClassLoader classloader : classloaders) {
            URL resource = classloader.getResource(name);
            if (resource != null) {
                return resource;
            }
            // Resource == null means not found, so let the next one try.
        }
        // We could not get the resource from the provided entity class loaders, so proceed with 'normal' behavior.
        return super.getResource(name);
    }

}
