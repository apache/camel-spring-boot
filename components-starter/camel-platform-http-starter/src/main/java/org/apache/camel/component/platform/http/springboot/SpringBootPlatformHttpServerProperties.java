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
package org.apache.camel.component.platform.http.springboot;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Spring Boot HTTP server serving the platform-http endpoints.
 */
@ConfigurationProperties(prefix = "camel.component.platform-http.server")
public class SpringBootPlatformHttpServerProperties {

    /**
     * Whether the temporary files, that multipart file uploads are written to, are deleted when the exchange is done
     * being routed. The uploaded file is copied out of the servlet container into the servlet temp directory so that
     * it stays readable after the HTTP request has completed, which makes Camel the owner of that copy. Turn this off
     * only if the route hands the file over to something that reads it after the exchange has completed - the route is
     * then responsible for deleting the file.
     */
    private boolean deleteUploadedFilesOnEnd = true;

    public boolean isDeleteUploadedFilesOnEnd() {
        return deleteUploadedFilesOnEnd;
    }

    public void setDeleteUploadedFilesOnEnd(boolean deleteUploadedFilesOnEnd) {
        this.deleteUploadedFilesOnEnd = deleteUploadedFilesOnEnd;
    }
}
