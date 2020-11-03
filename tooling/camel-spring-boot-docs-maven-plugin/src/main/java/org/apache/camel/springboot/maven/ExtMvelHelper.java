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
package org.apache.camel.springboot.maven;

import java.nio.file.Path;

import org.apache.camel.maven.packaging.MvelHelper;
import org.apache.camel.tooling.model.ArtifactModel;
import org.apache.camel.tooling.model.ComponentModel;
import org.apache.camel.tooling.model.DataFormatModel;
import org.apache.camel.tooling.model.LanguageModel;
import org.apache.camel.tooling.model.OtherModel;
import org.apache.camel.tooling.model.SupportLevel;
import org.apache.camel.tooling.util.Strings;

public class ExtMvelHelper {

    private final Path extensionsDocPath;

    public ExtMvelHelper(Path extensionsDocPath) {
        this.extensionsDocPath = extensionsDocPath;
    }

    public static String escape(final String raw) {
        return MvelHelper.escape(raw);
    }

    public String getFirstVersionShort(Object model) {
        String version = (String) invokeGetter(model, "getFirstVersion");
        return org.apache.camel.tooling.model.Strings.cutLastZeroDigit(version);
    }

    public String getSupportLevel(Object model) {
        if (model instanceof ArtifactModel) {
            ArtifactModel am = (ArtifactModel) model;
            if (am.getSupportLevel() != null) {
                return am.getSupportLevel().name();
            } else {
                return SupportLevel.Preview.name();
            }
        }
        return null;
    }

    public String getDocLink(Object model) {
        if (localDocExists(model)) {
            return getLocalDocLink(model);
        } else if (model instanceof ComponentModel) {
            return String.format("xref:latest@components::%s-component.adoc", invokeGetter(model, "getScheme"));
        } else if (model instanceof DataFormatModel) {
            return String.format("xref:latest@components:dataformats:%s.adoc", invokeGetter(model, "getName"));
        } else if (model instanceof LanguageModel) {
            return String.format("xref:latest@components:languages:%s.adoc", invokeGetter(model, "getName"));
        } else if (model instanceof OtherModel) {
            return String.format("xref:latest@components:others:%s.adoc", invokeGetter(model, "getName"));
        } else {
            return null;
        }
    }

    private Object invokeGetter(Object model, String method) {
        try {
            return model.getClass().getMethod(method)
                    .invoke(model);
        } catch (Exception e) {
            throw new RuntimeException("Unable to access " + method + " from " + model, e);
        }
    }

    private boolean localDocExists(Object model) {
        Path path = extensionsDocPath.resolve(getSpringBootDocName(model));
        return path.toFile().exists();
    }

    private String getLocalDocLink(Object model) {
        return "xref:components-starter/" + getSpringBootDocName(model);
    }

    private String getSpringBootDocName(Object model) {
        return Strings.after((String) invokeGetter(model, "getArtifactId"), "camel-") + ".adoc";
    }
}
