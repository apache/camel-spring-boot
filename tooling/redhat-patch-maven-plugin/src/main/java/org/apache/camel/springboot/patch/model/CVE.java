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

public class CVE {

    private String id;
    private String description;
    private String cveLink;
    private String bzLink;

    private final List<AffectedArtifactSpec> affected = new LinkedList<>();

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<AffectedArtifactSpec> getAffected() {
        return affected;
    }

    public String getCveLink() {
        return cveLink;
    }

    public void setCveLink(String cveLink) {
        this.cveLink = cveLink;
    }

    public String getBzLink() {
        return bzLink;
    }

    public void setBzLink(String bzLink) {
        this.bzLink = bzLink;
    }

    @Override
    public String toString() {
        String cveLink = this.cveLink != null && !this.cveLink.trim().equals("") ? this.cveLink : null;
        String bzLink = this.bzLink != null && !this.bzLink.trim().equals("") ? this.bzLink : null;
        if (cveLink != null && bzLink != null) {
            return id + ": " + description + " (" + cveLink + ", " + bzLink + ")";
        } else if (cveLink != null) {
            return id + ": " + description + " (" + cveLink + ")";
        } else if (bzLink != null) {
            return id + ": " + description + " (" + bzLink + ")";
        } else {
            return id + ": " + description;
        }
    }

}
