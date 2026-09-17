/*
 * Copyright 2005-2020 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.springboot.patch.model;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.aether.version.VersionRange;

public class PatchMetadata {

    private long timestamp;
    private String productGroupId;
    private String productArtifactId;
    private VersionRange productVersionRange;

    private final List<CVE> cves = new LinkedList<>();
    private final List<Fix> fixes = new LinkedList<>();

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setProductGroupId(String productGroupId) {
        this.productGroupId = productGroupId;
    }

    public void setProductArtifactId(String productArtifactId) {
        this.productArtifactId = productArtifactId;
    }

    public void setProductVersionRange(VersionRange productVersionRange) {
        this.productVersionRange = productVersionRange;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getProductGroupId() {
        return productGroupId;
    }

    public String getProductArtifactId() {
        return productArtifactId;
    }

    public VersionRange getProductVersionRange() {
        return productVersionRange;
    }

    public List<CVE> getCves() {
        return cves;
    }

    public List<Fix> getFixes() {
        return fixes;
    }

}
