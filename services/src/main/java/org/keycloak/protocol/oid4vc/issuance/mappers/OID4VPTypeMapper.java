/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oid4vc.issuance.mappers;

import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oid4vc.OID4VCClientRegistrationProviderFactory;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Allows to add types to the credential subject
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class OID4VPTypeMapper extends OID4VPMapper {

    public static final String MAPPER_ID = "oid4vc-vc-type-mapper";
    public static final String TYPE_KEY = "vcTypeProperty";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();

    public OID4VPTypeMapper() {
        super();
        ProviderConfigProperty vcTypePropertyNameConfig = new ProviderConfigProperty();
        vcTypePropertyNameConfig.setName(TYPE_KEY);
        vcTypePropertyNameConfig.setLabel("Verifiable Credential Type");
        vcTypePropertyNameConfig.setHelpText("Type of the credential.");
        vcTypePropertyNameConfig.setType(ProviderConfigProperty.STRING_TYPE);
        CONFIG_PROPERTIES.add(vcTypePropertyNameConfig);

    }

    @Override
    protected List<ProviderConfigProperty> getIndividualConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    public static ProtocolMapperModel create(String name, String subjectId) {
        var mapperModel = new ProtocolMapperModel();
        mapperModel.setName(name);
        Map<String, String> configMap = new HashMap<>();
        configMap.put(SUPPORTED_CREDENTIALS_KEY, "VerifiableCredential");
        mapperModel.setConfig(configMap);
        mapperModel.setProtocol(OID4VCClientRegistrationProviderFactory.PROTOCOL_ID);
        mapperModel.setProtocolMapper(MAPPER_ID);
        return mapperModel;
    }

    public void setClaimsForCredential(VerifiableCredential verifiableCredential,
                                       UserSessionModel userSessionModel) {
        // remove duplicates
        Set<String> types = new HashSet<>();
        if (verifiableCredential.getType() != null) {
            types = new HashSet<>(verifiableCredential.getType());
        }
        types.add(mapperModel.getConfig().get(TYPE_KEY));
        verifiableCredential.setType(new ArrayList<>(types));
    }

    @Override
    public void setClaimsForSubject(Map<String, Object> claims, UserSessionModel userSessionModel) {
        // nothing to do for the mapper.
    }

    @Override
    public String getDisplayType() {
        return "Credential Type Mapper";
    }

    @Override
    public String getHelpText() {
        return "Assigns a type to the credential.";
    }

    @Override
    public String getId() {
        return MAPPER_ID;
    }
}