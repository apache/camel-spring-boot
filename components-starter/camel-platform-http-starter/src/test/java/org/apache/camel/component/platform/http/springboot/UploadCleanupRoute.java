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

import jakarta.activation.DataHandler;
import jakarta.activation.FileDataSource;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.attachment.AttachmentMessage;
import org.apache.camel.builder.RouteBuilder;

import java.io.File;
import java.nio.file.Path;

/**
 * Reports back, in the response headers, where the accepted multipart uploads were written to and whether they were
 * still readable while the exchange was being routed. Used to assert the temporary file handling of
 * {@link SpringBootPlatformHttpBinding}.
 */
public class UploadCleanupRoute extends RouteBuilder {

    static final String UPLOAD_PATHS = "uploadPaths";
    static final String ATTACHMENT_COUNT = "attachmentCount";
    static final String EXISTED_DURING_ROUTING = "existedDuringRouting";
    static final String FILE_PATH_HEADER = "filePathHeader";
    static final String BODY_PATH = "bodyPath";

    @Override
    public void configure() {
        from("platform-http:/upload")
                .routeId("upload")
                .process(exchange -> {
                    AttachmentMessage am = exchange.getMessage(AttachmentMessage.class);
                    StringBuilder paths = new StringBuilder();
                    boolean existed = true;
                    int count = 0;
                    if (am.getAttachments() != null) {
                        for (DataHandler dataHandler : am.getAttachments().values()) {
                            File file = ((FileDataSource) dataHandler.getDataSource()).getFile();
                            if (count > 0) {
                                paths.append(',');
                            }
                            paths.append(file.getAbsolutePath());
                            existed = existed && file.isFile();
                            count++;
                        }
                    }

                    Message message = exchange.getMessage();
                    Object body = message.getBody();
                    Object filePath = message.getHeader(Exchange.FILE_PATH);
                    message.setHeader(ATTACHMENT_COUNT, String.valueOf(count));
                    message.setHeader(UPLOAD_PATHS, paths.toString());
                    message.setHeader(EXISTED_DURING_ROUTING, String.valueOf(existed));
                    message.setHeader(FILE_PATH_HEADER, filePath == null ? "" : filePath.toString());
                    message.setHeader(BODY_PATH, body instanceof Path path ? path.toAbsolutePath().toString() : "");
                    message.setBody("ok");
                });
    }
}
