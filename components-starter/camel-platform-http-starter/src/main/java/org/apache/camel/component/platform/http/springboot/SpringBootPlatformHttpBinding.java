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
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.camel.Exchange;
import org.apache.camel.InvalidPayloadException;
import org.apache.camel.Message;
import org.apache.camel.NoTypeConversionAvailableException;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.StreamCache;
import org.apache.camel.TypeConverter;
import org.apache.camel.attachment.AttachmentMessage;
import org.apache.camel.attachment.CamelFileDataSource;
import org.apache.camel.attachment.DefaultAttachmentMessage;
import org.apache.camel.component.platform.http.PlatformHttpEndpoint;
import org.apache.camel.component.platform.http.spi.Method;
import org.apache.camel.converter.stream.CachedOutputStream;
import org.apache.camel.http.base.HttpHelper;
import org.apache.camel.http.common.DefaultHttpBinding;
import org.apache.camel.support.ExchangeHelper;
import org.apache.camel.util.FileUtil;
import org.apache.camel.util.IOHelper;
import org.apache.camel.util.URISupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class SpringBootPlatformHttpBinding extends DefaultHttpBinding {
    private static final Logger LOG = LoggerFactory.getLogger(SpringBootPlatformHttpBinding.class);

    private static final String CONTENT_TYPE_FORM_URLENCODED = "application/x-www-form-urlencoded";
    private static final List<Method> METHODS_WITH_BODY_ALLOWED = List.of(Method.POST,
            Method.PUT, Method.PATCH, Method.DELETE);
    private static final List<Method> METHODS_WITH_REQUEST_ALREADY_READ = List.of(Method.PUT,
            Method.PATCH, Method.DELETE);

    protected void populateRequestParameters(HttpServletRequest request, Message message) {
        super.populateRequestParameters(request, message);
        String path = request.getRequestURI();
        // skip leading slash
        if (path != null && path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path != null) {
            PlatformHttpEndpoint endpoint = (PlatformHttpEndpoint) message.getExchange().getFromEndpoint();
            String consumerPath = endpoint.getPath();
            if (consumerPath != null && consumerPath.startsWith("/")) {
                consumerPath = consumerPath.substring(1);
            }
            if (useRestMatching(consumerPath)) {
                HttpHelper.evalPlaceholders(message.getHeaders(), path, consumerPath);
            }
        }
    }

    private boolean useRestMatching(String path) {
        return path.indexOf('{') > -1;
    }

    @Override
    protected void populateAttachments(HttpServletRequest request, Message message) {
        // check if there is multipart files, if so will put it into DataHandler
        if (request instanceof MultipartHttpServletRequest multipartHttpServletRequest) {
            File tmpFolder = (File) request.getServletContext().getAttribute(ServletContext.TEMPDIR);
            boolean isSingleAttachment = multipartHttpServletRequest.getFileMap() != null &&
                    multipartHttpServletRequest.getFileMap().keySet().size() == 1;
            message.setHeader(Exchange.ATTACHMENTS_SIZE, multipartHttpServletRequest.getFileMap().keySet().size());
            multipartHttpServletRequest.getFileMap().forEach((name, multipartFile) -> {
                try {
                    Path uploadedTmpFile = Paths.get(tmpFolder.getPath(), UUID.randomUUID().toString());
                    multipartFile.transferTo(uploadedTmpFile);

                    if (name != null) {
                        name = name.replaceAll("[\n\r\t]", "_");
                    }

                    boolean accepted = true;

                    if (getFileNameExtWhitelist() != null) {
                        String ext = FileUtil.onlyExt(name);
                        if (ext != null) {
                            ext = ext.toLowerCase(Locale.US);
                            if (!getFileNameExtWhitelist().equals("*") && !getFileNameExtWhitelist().contains(ext)) {
                                accepted = false;
                            }
                        }
                    }

                    if (accepted) {
                        AttachmentMessage am = new DefaultAttachmentMessage(message);
                        File uploadedFile = uploadedTmpFile.toFile();
                        am.addAttachment(name, new DataHandler(new CamelFileDataSource(uploadedFile, name)));

                        // populate body in case there is only one attachment
                        if (isSingleAttachment) {
                            message.setHeader(Exchange.FILE_PATH, uploadedFile.getAbsolutePath());
                            message.setHeader(Exchange.FILE_LENGTH, multipartFile.getSize());
                            message.setHeader(Exchange.FILE_NAME, multipartFile.getOriginalFilename());
                            if (multipartFile.getContentType() != null) {
                                message.setHeader(Exchange.FILE_CONTENT_TYPE, multipartFile.getContentType());
                            }
                            message.setBody(uploadedTmpFile);
                        }
                    } else {
                        LOG.debug(
                                "Cannot add file as attachment: {} because the file is not accepted according to fileNameExtWhitelist: {}",
                                name, getFileNameExtWhitelist());
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    public Object parseBody(HttpServletRequest request, Message message) throws IOException {
        if (request instanceof StandardMultipartHttpServletRequest ||
                // In case of Spring FormContentFilter
                requestAlreadyRead(request)) {
            return null;
        }

        return super.parseBody(request, message);
    }

    @Override
    public void readRequest(HttpServletRequest request, Message message) {
        super.readRequest(request, message);

        populateMultiFormData(request, message);
    }

    private static void populateMultiFormData(HttpServletRequest request, Message message) {
        if (((PlatformHttpEndpoint) message.getExchange().getFromEndpoint()).isPopulateBodyWithForm() &&
                METHODS_WITH_BODY_ALLOWED.contains(Method.valueOf(request.getMethod())) &&
                (message.getBody() instanceof StreamCache ||
                        (message.getBody() == null && !"POST".equals(request.getMethod()))) &&
                request.getContentType() != null &&
                request.getContentType().contains(CONTENT_TYPE_FORM_URLENCODED)) {
            // FormContentFilter is NOT executed for POST requests
            if ("POST".equals(request.getMethod())) {
                String body = message.getBody(String.class);
                try {
                    message.setBody(URISupport.parseQuery(body));
                } catch (URISyntaxException e) {
                    LOG.error("Cannot parse body: {}", body, e);
                    throw new RuntimeCamelException(e);
                }
            } else {
                // FormContentFilter is executed, the request.getReader is already read
                // and the parameters can be found in the parameterMap
                message.setBody(request.getParameterMap());
            }
        }
    }

    private boolean requestAlreadyRead(HttpServletRequest request) {
        return Objects.nonNull(request.getContentType()) &&
                request.getContentType().contains(CONTENT_TYPE_FORM_URLENCODED) &&
                METHODS_WITH_REQUEST_ALREADY_READ.contains(Method.valueOf(request.getMethod()));
    }

    protected void doWriteDirectResponse(Message message, HttpServletResponse response, Exchange exchange) throws IOException {
        String contentType = (String)message.getHeader("Content-Type", String.class);
        if ("application/x-java-serialized-object".equals(contentType)) {
            if (!isAllowJavaSerializedObject() && !this.isTransferException()) {
                throw new RuntimeCamelException("Content-type application/x-java-serialized-object is not allowed");
            } else {
                try {
                    Object object = message.getMandatoryBody(Serializable.class);
                    org.apache.camel.http.common.HttpHelper.writeObjectToServletResponse(response, object);
                } catch (InvalidPayloadException var19) {
                    InvalidPayloadException e = var19;
                    throw new IOException(e);
                }
            }
        } else {
            InputStream is = null;
            if (this.checkChunked(message, exchange)) {
                is = message.getBody(InputStream.class);
            } else if (!this.isText(contentType)) {
                is = exchange.getContext().getTypeConverter().tryConvertTo(InputStream.class, message.getBody());
            }

            int len;
            if (is != null) {
                ServletOutputStream os = response.getOutputStream();
                if (!this.checkChunked(message, exchange)) {
                    CachedOutputStream stream = new CachedOutputStream(exchange);

                    try {
                        len = this.copyStream(is, stream, response.getBufferSize());
                        response.setContentLength(len);
                        OutputStream current = stream.getCurrentStream();
                        if (current instanceof ByteArrayOutputStream) {
                            ByteArrayOutputStream bos = (ByteArrayOutputStream)current;
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Streaming (direct) response in non-chunked mode with content-length {}", len);
                            }

                            bos.writeTo(os);
                        } else {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Streaming response in non-chunked mode with content-length {} and buffer size: {}", len, len);
                            }

                            this.copyStream(stream.getInputStream(), os, len);
                        }
                    } finally {
                        IOHelper.close(new Closeable[]{is, os});
                        stream.close();
                    }
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Streaming response in chunked mode with buffer size {}", response.getBufferSize());
                    }

                    this.copyStream(is, os, response.getBufferSize());
                }
            } else {
                Object body = message.getBody();
                if (body instanceof String) {
                    String data = message.getBody(String.class);

                    if (data != null) {
                        String charset = ExchangeHelper.getCharsetName(exchange, true);
                        len = data.getBytes(charset).length;
                        response.setCharacterEncoding(charset);
                        response.setContentLength(len);
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Writing response in non-chunked mode as plain text with content-length {} and buffer size: {}", len, response.getBufferSize());
                        }

                        try {
                            response.getWriter().print(data);
                        } finally {
                            response.getWriter().flush();
                        }
                    }
                } else if (body instanceof InputStream) {
                    InputStream bodyIS = message.getBody(InputStream.class);
                    bodyIS.transferTo(response.getOutputStream());
                } else {
                    final TypeConverter tc = exchange.getContext().getTypeConverter();
                    // Try to convert to ByteBuffer for performance reason
                    final ByteBuffer bb = tc.tryConvertTo(ByteBuffer.class, exchange, body);
                    if (bb != null) {
                        response.getOutputStream().write(bb.array());
                    } else {
                        try {
                            final InputStream bodyIS = tc.mandatoryConvertTo(InputStream.class, exchange, body);
                            bodyIS.transferTo(response.getOutputStream());
                        } catch (NoTypeConversionAvailableException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected String getRawPath(HttpServletRequest request) {
        String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        String servletPath = request.getServletPath() == null ? "" : request.getServletPath();
        return contextPath + servletPath;
    }
}
