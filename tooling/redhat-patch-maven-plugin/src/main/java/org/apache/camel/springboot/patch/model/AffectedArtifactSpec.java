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

import java.util.regex.Pattern;

import org.apache.maven.model.Dependency;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AffectedArtifactSpec {

    public static final Logger LOG = LoggerFactory.getLogger(AffectedArtifactSpec.class);
    public static final GenericVersionScheme GVS = new GenericVersionScheme();

    private String groupIdSpec;
    private String artifactIdSpec;
    // TOCHECK: for now we're sticking to version ranges and no direct version
    private transient VersionRange versionRange;
    private String versionRangeString;
    private transient Version fixVersion;
    private String fixVersionString;

    private transient Pattern groupIdSpecPattern;
    private transient Pattern artifactIdSpecPattern;

    public boolean matches(Dependency dependency) {
        String g = dependency.getGroupId();
        String a = dependency.getArtifactId();
        Version v = null;
        try {
            if (dependency.getVersion() != null) {
                v = GVS.parseVersion(dependency.getVersion());
            }
        } catch (InvalidVersionSpecificationException e) {
            LOG.warn("[PATCH] Problem parsing managed dependency version: {}", e.getMessage(), e);
        }

        if (!(groupIdSpecPattern == null ? g.equals(groupIdSpec) : groupIdSpecPattern.matcher(g).matches())) {
            return false;
        }
        if (!(artifactIdSpecPattern == null ? a.equals(artifactIdSpec) : artifactIdSpecPattern.matcher(a).matches())) {
            return false;
        }
        if (v != null && versionRange != null && !(versionRange.containsVersion(v))) {
            return false;
        }
        return true;
    }

    public String getGroupIdSpec() {
        return groupIdSpec;
    }

    public void setGroupIdSpec(String groupIdSpec) {
        this.groupIdSpec = groupIdSpec;
        if (groupIdSpec.contains("*")) {
            this.groupIdSpecPattern = Pattern.compile(groupIdSpec.replaceAll("\\.", ".").replaceAll("\\*", ".*"));
        }
    }

    public String getArtifactIdSpec() {
        return artifactIdSpec;
    }

    public void setArtifactIdSpec(String artifactIdSpec) {
        this.artifactIdSpec = artifactIdSpec;
        if (artifactIdSpec.contains("*")) {
            this.artifactIdSpecPattern = Pattern.compile(artifactIdSpec.replaceAll("\\.", ".").replaceAll("\\*", ".*"));
        }
    }

    public VersionRange getVersionRange() {
        return versionRange;
    }

    public String getVersionRangeString() {
        return versionRangeString;
    }

    public void setVersionRange(VersionRange versionRange) {
        this.versionRange = versionRange;
        this.versionRangeString = versionRange == null ? null : versionRange.toString();
    }

    public Version getFixVersion() {
        return fixVersion;
    }

    public String getFixVersionString() {
        return fixVersionString;
    }

    public void setFixVersion(Version fixVersion) {
        this.fixVersion = fixVersion;
        this.fixVersionString = fixVersion == null ? null : fixVersion.toString();
    }

    @Override
    public String toString() {
        return String.format("%s/%s/%s -> %s", groupIdSpec, artifactIdSpec, versionRange, fixVersion);
    }

}
