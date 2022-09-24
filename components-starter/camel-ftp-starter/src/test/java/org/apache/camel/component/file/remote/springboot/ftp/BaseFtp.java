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
package org.apache.camel.component.file.remote.springboot.ftp;

import org.apache.camel.component.file.remote.springboot.AbstractBaseFtp;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.nio.file.Path;


public class BaseFtp extends AbstractBaseFtp {
    protected static final String AUTH_VALUE_SSL = "SSLv3";
    protected static final String AUTH_VALUE_TLS = "TLSv1.3";

    @RegisterExtension
    static FtpEmbeddedService service = new FtpEmbeddedService();

    @Override
    protected int getPort() {
        return service.getPort();
    }

    protected Path ftpFile(String file) {
        return service.getFtpRootDir().resolve(file);
    }

    protected String getFtpUrl(String user, String password) {
        StringBuilder url = new StringBuilder("ftp://");
        url.append(user == null ? "" : user + "@");
        url.append("localhost:" + service.getPort() + "/");
        url.append(password == null ? "" : "?password=" + password);
        return url.toString();
    }

}
