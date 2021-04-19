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

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.camel.maven.packaging.MvelHelper;
import org.apache.camel.tooling.model.ArtifactModel;
import org.apache.camel.tooling.model.BaseModel;
import org.apache.camel.tooling.model.ComponentModel;
import org.apache.camel.tooling.model.DataFormatModel;
import org.apache.camel.tooling.model.LanguageModel;
import org.apache.camel.tooling.model.OtherModel;
import org.apache.camel.tooling.model.SupportLevel;

public class ExtMvelHelper {

    private final Path extensionsDocPath;

    public ExtMvelHelper(Path extensionsDocPath) {
        this.extensionsDocPath = extensionsDocPath;
    }

    public static String escape(final String raw) {
        return MvelHelper.escape(raw);
    }

    public String getFirstVersionShort(BaseModel<?> model) {
        String version = model.getFirstVersion();
        return org.apache.camel.tooling.model.Strings.cutLastZeroDigit(version);
    }

    public String getSupportLevel(ArtifactModel<?> model) {
        final SupportLevel supportLevel = model.getSupportLevel();
        if (supportLevel != null) {
            return model.getSupportLevel().name();
        }

        return SupportLevel.Preview.name();
    }

    public String getDocLink(ArtifactModel<?> model) {
        if (isLocalComponent(model)) {
            return String.format("xref:%s.adoc", ((ArtifactModel<?>) model).getName());
        } else if (model instanceof ComponentModel) {
            return String.format("xref:3.9.x@latest@components::%s-component.adoc", ((ComponentModel) model).getScheme());
        } else if (model instanceof DataFormatModel) {
            return String.format("xref:3.9.x@latest@components:dataformats:%s-dataformat.adoc", ((DataFormatModel) model).getName());
        } else if (model instanceof LanguageModel) {
            return String.format("xref:3.9.x@latest@components:languages:%s-language.adoc", ((LanguageModel) model).getName());
        } else if (model instanceof OtherModel) {
            return String.format("xref:3.9.x@latest@components:others:%s.adoc", ((OtherModel) model).getName());
        } else {
            return null;
        }
    }

    private boolean isLocalComponent(ArtifactModel<?> model) {
        return Files.exists(extensionsDocPath.resolve(((ArtifactModel<?>) model).getName() + ".adoc"));
    }

}
