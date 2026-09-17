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

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.VersionRange;

import static org.apache.camel.springboot.patch.model.AffectedArtifactSpec.GVS;

public class ProductVersion {

    private final int major;
    private int minor = 0;
    private String qualifier;

    public ProductVersion(String version) {
//        DefaultArtifactVersion v = new DefaultArtifactVersion(version);
        // 7.8.0 will be parsed correctly
        // 7.8.0-fuse-xxx will be parsed correctly
        // 7.8.0.1 won't be parsed correctly
        // 7.8.0.redhat-xxx will give correct major.minor, but qualifier will be "redhat" instead of "redhat-xxx"
        // 7.8.0-redhat-xxx will be parsed correctly
        //
        // but because we can't reliably get full qualifier, we'll parse the vesion manually

        StringTokenizer st = new StringTokenizer(version, ".");
        List<Integer> digits = new ArrayList<>();
        StringBuilder qualifier = new StringBuilder();
        boolean inQualifier = false;
        while (st.hasMoreTokens()) {
            String part = st.nextToken();
            if (!inQualifier) {
                for (char c : part.toCharArray()) {
                    if (!Character.isDigit(c)) {
                        // this part and the rest will be the qualifier
                        inQualifier = true;
                        break;
                    }
                }
            }
            if (inQualifier) {
                qualifier.append('.').append(part);
            } else {
                digits.add(Integer.parseInt(part));
            }
        }

        if (digits.size() == 0) {
            // case of "1-redhat" version...
            major = 0;
            minor = 0;
        } else {
            major = digits.get(0);
            if (digits.size() > 1) {
                minor = digits.get(1);
            }
        }
        this.qualifier = qualifier.toString();
        if (this.qualifier.length() > 0) {
            // skip the '.'
            this.qualifier = this.qualifier.substring(1);
        }
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public String getQualifier() {
        return qualifier;
    }

    public boolean canUse(VersionRange productVersionRange) {
        try {
            return productVersionRange.containsVersion(GVS.parseVersion(String.format("%d.%d", major, minor)));
        } catch (InvalidVersionSpecificationException ignored) {
            return false;
        }
    }

}
