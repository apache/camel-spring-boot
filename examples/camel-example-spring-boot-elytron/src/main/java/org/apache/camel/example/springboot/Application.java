/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.example.springboot;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.wildfly.security.auth.permission.LoginPermission;
import org.wildfly.security.auth.realm.token.TokenSecurityRealm;
import org.wildfly.security.auth.realm.token.validator.JwtValidator;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.authz.RoleMapper;
import org.wildfly.security.authz.Roles;
import org.wildfly.security.permission.PermissionVerifier;

//CHECKSTYLE:OFF
/**
 */
@SpringBootApplication
@ImportResource({"classpath:spring/camel-context.xml"})
public class Application {

    @Autowired
    private KeyPair keyPair;

    /**
     * A main method to start this application.
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }


    @Bean(name = "mySecurityDomainBuilder")
    public SecurityDomain.Builder createSecurityDomainBuilder() throws Exception {

            SecurityDomain.Builder builder = SecurityDomain.builder()
                    .setDefaultRealmName("bearerRealm");

            builder.addRealm("bearerRealm", TokenSecurityRealm.builder().principalClaimName("username")
                .validator(JwtValidator.builder().publicKey(getKeyPair().getPublic()).build()).build())
                .build();

            builder.setPermissionMapper((principal, roles) -> PermissionVerifier.from(new LoginPermission()));
            builder.setRoleMapper(RoleMapper.constant(Roles.of("guest")).or((roles) -> roles));


            return builder;
    }

    @Bean(name = "myKeyPair")
    public KeyPair getKeyPair() throws NoSuchAlgorithmException {
        if (keyPair == null) {
            keyPair = generateKeyPair();
        }
        return keyPair;
    }

    private KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        return KeyPairGenerator.getInstance("RSA").generateKeyPair();
    }


}
//CHECKSTYLE:ON
