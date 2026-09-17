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

public class Fix {

    private String id;
    private String description;
    private String link;

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

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    @Override
    public String toString() {
        String link = this.link != null && !this.link.trim().equals("") ? this.link : null;
        if (link != null) {
            return id + ": " + description + " (" + link + ")";
        } else {
            return id + ": " + description;
        }
    }

}
