/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage;

import java.util.Map;
import java.util.Set;

/**
 *
 * @author hmlnarik
 */
public interface MapStorage<K, V> {

    V get(K key);

    V put(K key, V value);

    V putIfAbsent(K key, V value);

    V remove(K key);

    Set<Map.Entry<K,V>> entrySet();

}
