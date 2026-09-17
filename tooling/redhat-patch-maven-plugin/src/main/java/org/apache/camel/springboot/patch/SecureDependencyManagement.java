/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.camel.springboot.patch;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.camel.springboot.patch.extensions.ZipWagon;
import org.apache.camel.springboot.patch.model.AffectedArtifactSpec;
import org.apache.camel.springboot.patch.model.CVE;
import org.apache.camel.springboot.patch.model.Fix;
import org.apache.camel.springboot.patch.model.PatchMetadata;
import org.apache.camel.springboot.patch.model.ProductVersion;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.interpolation.StringVisitorModelInterpolator;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.MetadataRequest;
import org.eclipse.aether.resolution.MetadataResult;
import org.slf4j.Logger;

/**
 * <p>The core mechanism that alters dependencyManagement and dependencies of the project where this plugin is
 * declared.</p>
 *
 * <p>The point is to fetch relevant metadata from any of the configured Maven repositories and use it to alter
 * the dependencies.</p>
 *
 * <p>This plugin was originally created for Fuse 7 and was supporting two different BOMs. Here we support only one.</p>
 */
@Component(role = AbstractMavenLifecycleParticipant.class)
public class SecureDependencyManagement extends AbstractMavenLifecycleParticipant {

    @Requirement
    private Logger logger;

    @Requirement
    private PlexusContainer container;

    @Requirement
    RepositorySystem repo;

    // coordinates of the plugin itself
    private String pluginGroupId;
    private String pluginArtifactId;

    // coordinates of the patch metadata for Camel Spring Boot
    private String mdCsbGroupId;
    private String mdCsbArtifactId;
    private String mdCsbType;

    // coordinates of the official Camel Spring Boot BOM
    private String bomCsbGroupId;
    private String bomCsbArtifactId;

    private File tmpDir = null;

    @Override
    public void afterSessionStart(MavenSession session) throws MavenExecutionException {
        super.afterSessionStart(session);
    }

    @Override
    public void afterSessionEnd(MavenSession session) throws MavenExecutionException {
        super.afterSessionEnd(session);
        if (tmpDir != null) {
            cleanupRepository(tmpDir);
        }
    }

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        if (session == null) {
            return;
        }
        long ts = System.currentTimeMillis();
        configureProperties(session);

        if (shouldSkip(session)) {
            return;
        }

        logger.info("\n\n========== Red Hat Maven patching ==========\n");

        try {
            // detect which BOM we're using
            // we have to review "original model", because at this stage, the effective <dependencyManagement>
            // has already its bom-imports "resolved"
            // (by org.apache.maven.model.building.DefaultModelBuilder.importDependencyManagement())
            Dependency bom = findProductBOM(session);
            if (bom == null) {
                return;
            }

            List<RemoteRepository> repositories = configureRepositories(session);

            List<MetadataRequest> metadataRequests = configurePatchMetadataRequest(session, bom, repositories);
            List<MetadataResult> metadataResults = repo.resolveMetadata(session.getRepositorySession(), metadataRequests);

            // The metadata result may come from different sources (latest /metadata/versioning/lastUpdated wins), but
            // also may contain metadata versions for different CSB versions, like:
            // <?xml version="1.0" encoding="UTF-8"?>
            // <metadata>
            //   <groupId>com.redhat.camel.springboot.platform</groupId>
            //   <artifactId>redhat-camel-spring-boot-patch-metadata</artifactId>
            //   <versioning>
            //     <release>3.21.0.redhat-00001</release>
            //     <versions>
            //       <version>3.21.0.redhat-00001</version>
            //       <version>3.20.0.redhat-00005</version>
            //       <version>3.20.0.redhat-00001</version>
            //     </versions>
            //     <lastUpdated>20230101010101</lastUpdated>
            //   </versioning>
            // </metadata>
            // thus we have to find proper version of patch metadata depending on which version we're running in
            // (in terms of used product BOM)
            String version = findLatestMetadataVersion(bom, metadataResults);
            if (version == null) {
                logger.warn("[PATCH] Can't find latest patch metadata for {} in any of the configured repositories.",
                        String.format("%s/%s/%s BOM", bom.getGroupId(), bom.getArtifactId(), bom.getVersion()));
                if (!logger.isDebugEnabled()) {
                    logger.warn("[PATCH] Please enable debug logging (-X) to see more details."
                            + " Perhaps the metadata was previously downloaded from different repository?");
                }
                return;
            }

            // we'll be looking for metadata specific to given current product
            ArtifactRequest request = configurePatchArtifactRequest(session, bom, version);
            request.setRepositories(repositories);

            ArtifactResult result;
            try {
                result = repo.resolveArtifact(session.getRepositorySession(), request);
                logger.info("[PATCH] Resolved patch descriptor: {}", result.getArtifact().getFile());
            } catch (ArtifactResolutionException e) {
                logger.warn("[PATCH] Unable to find patch metadata in any of the configured repositories");
                return;
            }

            PatchMetadata patch = readPatchMetadata(result.getArtifact().getFile());
            ProductVersion bomVersion = new ProductVersion(bom.getVersion());
            // validate if the metadata is for our project - just sanity check
            if (!bomVersion.canUse(patch.getProductVersionRange())) {
                logger.warn("[PATCH] Patch metadata is applicable to product version {} and can't be used with {}.",
                        patch.getProductVersionRange(),
                        String.format("%s/%s/%s BOM", bom.getGroupId(), bom.getArtifactId(), bom.getVersion()));
                return;
            }

            logger.info("[PATCH] Patch metadata found for {}/{}/{}",
                    patch.getProductGroupId(), patch.getProductArtifactId(), patch.getProductVersionRange());
            int cveCount = patch.getCves().size();
            int fixCount = patch.getFixes().size();
            if (cveCount > 0) {
                logger.info("[PATCH]  - patch contains {} CVE {}", cveCount, cveCount > 1 ? "fixes" : "fix");
            }
            if (fixCount > 0) {
                logger.info("[PATCH]  - patch contains {} patch {}", fixCount, fixCount > 1 ? "fixes" : "fix");
            }

            if (cveCount > 0) {
                logger.info("[PATCH] Processing managed dependencies to apply CVE fixes...");

                for (CVE cve : patch.getCves()) {
                    logger.info("[PATCH] - {}", cve);
                    for (AffectedArtifactSpec spec : cve.getAffected()) {
                        logger.info("[PATCH]   Applying change {}", spec);
                        for (MavenProject project : session.getProjects()) {
                            logger.info("[PATCH]   Project {}:{}", project.getGroupId(), project.getArtifactId());
                            if (project.getDependencyManagement() != null) {
                                for (Dependency dependency : project.getDependencyManagement().getDependencies()) {
                                    if (spec.matches(dependency)) {
                                        logger.info("[PATCH]    - managed dependency: {}/{}/{} -> {}",
                                                dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(),
                                                spec.getFixVersion());
                                        project.getManagedVersionMap().get(dependency.getManagementKey()).setResolvedVersion(spec.getFixVersion().toString());
                                        project.getManagedVersionMap().get(dependency.getManagementKey()).setVersion(spec.getFixVersion().toString());
                                        dependency.setVersion(spec.getFixVersion().toString());
                                    }
                                }
                            }
                            for (Dependency dependency : project.getDependencies()) {
                                if (spec.matches(dependency)) {
                                    logger.info("[PATCH]    - dependency: {}/{}/{} -> {}",
                                            dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(),
                                            spec.getFixVersion());
                                    dependency.setVersion(spec.getFixVersion().toString());
                                }
                            }
                        }
                    }
                }
            }
            if (fixCount > 0) {
                logger.info("[PATCH] Processing managed dependencies to apply patch fixes...");

                for (Fix fix : patch.getFixes()) {
                    logger.info("[PATCH] - {}", fix);
                    for (AffectedArtifactSpec spec : fix.getAffected()) {
                        logger.info("[PATCH]   Applying change {}", spec);
                        for (MavenProject project : session.getProjects()) {
                            logger.info("[PATCH]   Project {}:{}", project.getGroupId(), project.getArtifactId());
                            if (project.getDependencyManagement() != null) {
                                for (Dependency dependency : project.getDependencyManagement().getDependencies()) {
                                    if (spec.matches(dependency)) {
                                        logger.info("[PATCH]    - managed dependency: {}/{}/{} -> {}",
                                                dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(),
                                                spec.getFixVersion());
                                        project.getManagedVersionMap().get(dependency.getManagementKey()).setResolvedVersion(spec.getFixVersion().toString());
                                        project.getManagedVersionMap().get(dependency.getManagementKey()).setVersion(spec.getFixVersion().toString());
                                        dependency.setVersion(spec.getFixVersion().toString());
                                    }
                                }
                            }
                            for (Dependency dependency : project.getDependencies()) {
                                if (spec.matches(dependency)) {
                                    logger.info("[PATCH]    - dependency: {}/{}/{} -> {}",
                                            dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(),
                                            spec.getFixVersion());
                                    dependency.setVersion(spec.getFixVersion().toString());
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            logger.info("[PATCH] Done in " + (System.currentTimeMillis() - ts) + "ms\n\n=================================================\n");
        }
    }

    private void configureProperties(MavenSession session) throws MavenExecutionException {
        Properties props = new Properties();
        try (InputStream is = getClass().getResourceAsStream("/plugin.properties")) {
            props.load(is);
        } catch (IOException e) {
            throw new MavenExecutionException("Can't load plugin.properties",
                    session.getCurrentProject().getFile());
        }

        pluginGroupId = props.getProperty("plugin.groupId");
        pluginArtifactId = props.getProperty("plugin.artifactId");

        mdCsbGroupId = props.getProperty("patch-metadata.csb.groupId");
        mdCsbArtifactId = props.getProperty("patch-metadata.csb.artifactId");
        mdCsbType = props.getProperty("patch-metadata.csb.type");

        bomCsbGroupId = props.getProperty("bom.csb.groupId");
        bomCsbArtifactId = props.getProperty("bom.csb.artifactId");
    }

    private boolean shouldSkip(MavenSession session) {
        // <configuration>/<skip>
        Boolean skip = null;

        for (MavenProject p : session.getProjects()) {
            for (Plugin bp : p.getBuildPlugins()) {
                if ((pluginGroupId + ":" + pluginArtifactId).equals(bp.getKey())) {
                    if (bp.getConfiguration() instanceof Xpp3Dom) {
                        XmlPlexusConfiguration config = new XmlPlexusConfiguration((Xpp3Dom) bp.getConfiguration());
                        if (config.getChild("skip") != null) {
                            skip = "true".equalsIgnoreCase(config.getChild("skip").getValue());
                        }
                    }
                    break;
                }
            }
        }

        if (session.getUserProperties().containsKey("skipPatch")) {
            if (Boolean.parseBoolean(session.getUserProperties().getProperty("skipPatch"))) {
                skip = true;
            }
        }

        return skip != null && skip;
    }

    /**
     * <p>This method returns list of {@link RemoteRepository repositories} to be checked for patch metadata and
     * artifacts.</p>
     *
     * <p>Patch metadata and artifacts will be resolved using normal Maven mechanisms. We however provide special
     * kind of {@link RemoteRepository} that can be accessed through provided ZIP file (shipped as a
     * <em>patch</em>).</p>
     *
     * @param session
     * @return
     */
    private List<RemoteRepository> configureRepositories(MavenSession session) throws MavenExecutionException {
        List<RemoteRepository> repositories = new ArrayList<>();

        RemoteRepository zipRepository = null;

        String patch = session.getUserProperties().getProperty("patch");
        if ("true".equals(patch) || (patch != null && "".equals(patch.trim()))) {
            logger.warn("[PATCH] -Dpatch used, but patch location not specified. Are you sure correct -Dpatch=location is used?");
        } else {
            if (patch != null) {
                File pf = new File(patch);
                if (!pf.isFile()) {
                    logger.warn("[PATCH] Patch repository {} is not accessible. Project repositories will be used", patch);
                } else {
                    String canonicalPath = null;
                    try {
                        canonicalPath = pf.getCanonicalPath();
                    } catch (IOException ignored) {
                    }
                    logger.info("[PATCH] Reading metadata and artifacts from {}", canonicalPath);

                    // instead of "opening" and "closing" ZIP patch repository in ZipWagon,
                    // we'll unpack it now
                    tmpDir = ZipWagon.unpackPatchRepository(pf);

                    // ID of the repository is tricky question. If we use pf.getName() as ID (which looks nice), we
                    // may have problems later, because that's what Aether does with its _remote.repositories file!
                    // for example if the patch file was patch-1.zip and we used it as repo id, we'd get
                    // _remote.repositories file with:
                    //     camel-spring-boot-patch-metadata-<version>.xml>patch-1.zip=
                    // and next resolution of com.redhat.camel.springboot.platform:camel-spring-boot-patch-metadata:RELEASE with
                    // different patch of from remote repos would ONLY because the ID of the repo wouldn't match...
                    RemoteRepository.Builder zipRepositoryBuilder
                            = new RemoteRepository.Builder("csb-patch", "default", tmpDir.toURI().toString());
                    zipRepository = zipRepositoryBuilder.build();
                    repositories.add(zipRepository);
                }
            }
        }

        if (repositories.size() == 0) {
            for (org.apache.maven.model.Repository repo : session.getCurrentProject().getRepositories()) {
                String id = repo.getId() == null ? UUID.randomUUID().toString() : repo.getId();
                RemoteRepository.Builder builder = new RemoteRepository.Builder(id, repo.getLayout(), repo.getUrl());
                repositories.add(builder.build());
            }
            logger.info("[PATCH] Reading patch metadata and artifacts from {} project {}", repositories.size(),
                    repositories.size() > 1 ? "repositories" : "repository");
            for (RemoteRepository r : repositories) {
                logger.info("[PATCH]  - {}: {}", r.getId(), r.getUrl());
            }
        }

        if (zipRepository != null) {
            Repository r = new Repository();
            r.setId(zipRepository.getId());
            r.setLayout("default");
            try {
                r.setUrl(tmpDir.toURI().toURL().toString());
            } catch (MalformedURLException ignored) {
            }
            for (MavenProject mp : session.getAllProjects()) {
                mp.getRemoteProjectRepositories().add(zipRepository);
            }
        }

        return repositories;
    }

    /**
     * Iterate over all the projects in the session, check their {@code <dependencyManagement>} and
     * checks a list of all {@link Dependency} for them. These will have all the placeholders
     * resolved (because for example the version may have been parameterized) - this is required,
     * because {@link MavenProject#getOriginalModel()} was checked. Then, among all the dependencies we find the
     * <em>product BOM</em> to identify which product we're using.
     *
     * @param session
     * @return
     */
    private Dependency findProductBOM(MavenSession session) {
        Set<Dependency> result = new LinkedHashSet<>();

        for (MavenProject mp : session.getProjects()) {
            MavenProject _mp = mp;
            while (_mp != null) {
                DependencyManagement dm = _mp.getOriginalModel().getDependencyManagement();
                if (dm != null) {
                    List<Dependency> projectDependencies = new LinkedList<>();
                    for (Dependency d : dm.getDependencies()) {
                        if ("import".equals(d.getScope()) && "pom".equals(d.getType())) {
                            projectDependencies.add(d);
                        }
                    }
                    result.addAll(interpolate(session, _mp, projectDependencies));
                }
                if (_mp.getOriginalModel().getProfiles() != null) {
                    Set<String> activeProfiles = new HashSet<>();
                    if (_mp.getActiveProfiles() != null) {
                        for (Profile ap : _mp.getActiveProfiles()) {
                            activeProfiles.add(ap.getId());
                        }
                    }
                    for (Profile profile : _mp.getOriginalModel().getProfiles()) {
                        if (activeProfiles.contains(profile.getId())) {

                            DependencyManagement pdm = profile.getDependencyManagement();
                            if (pdm != null) {
                                List<Dependency> projectDependencies = new LinkedList<>();
                                for (Dependency d : pdm.getDependencies()) {
                                    if ("import".equals(d.getScope()) && "pom".equals(d.getType())) {
                                        projectDependencies.add(d);
                                    }
                                }
                                result.addAll(interpolate(session, _mp, projectDependencies));
                            }
                        }
                    }
                }
                _mp = _mp.getParent();
            }
        }

        Dependency csbBom = null;

        for (Dependency d : result) {
            if (bomCsbGroupId.equals(d.getGroupId()) && bomCsbArtifactId.equals(d.getArtifactId())) {
                csbBom = d;
            }
        }

        if (csbBom == null) {
            logger.info("[PATCH] No project in the reactor uses Camel on Spring Boot product BOM. Skipping patch processing.");
            return null;
        }

        return csbBom;
    }

    /**
     * Use Maven machinery to interpolate possible properties in ad-hoc model with BOM-dependencies.
     * @param session
     * @param mp
     * @param projectDependencies
     * @return
     */
    private List<Dependency> interpolate(MavenSession session, MavenProject mp, List<Dependency> projectDependencies) {
        // when operating on org.apache.maven.project.MavenProject.getOriginalModel(), we won't
        // get our model interpolated, so we have to do it ourselves
        Model m = new Model();
        DependencyManagement dm = new DependencyManagement();
        m.setDependencyManagement(dm);
        dm.getDependencies().addAll(projectDependencies);

        // properties from project hierarchy, starting from top
        Properties props = new Properties();
        Deque<MavenProject> projects = new LinkedList<>();
        MavenProject _mp = mp;
        while (_mp != null) {
            projects.push(_mp);
            _mp = _mp.getParent();
        }
        while (projects.size() > 0) {
            Properties _props = projects.pop().getProperties();
            if (_props != null) {
                props.putAll(_props);
            }
        }
        m.setProperties(props);

        StringVisitorModelInterpolator interpolator = new StringVisitorModelInterpolator();
        try {
            // maven 3.8.x
            Class<?> processorClass = getClass().getClassLoader().loadClass("org.apache.maven.model.interpolation.DefaultModelVersionProcessor");
            Class<?> parameterClass = getClass().getClassLoader().loadClass("org.apache.maven.model.interpolation.ModelVersionProcessor");
            Method setVersionPropertiesProcessor = interpolator.getClass().getMethod("setVersionPropertiesProcessor", parameterClass);
            setVersionPropertiesProcessor.invoke(interpolator, processorClass.getConstructor().newInstance());
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ignored) {
        }
        ModelBuildingRequest req = new DefaultModelBuildingRequest();
        req.getSystemProperties().putAll(session.getSystemProperties());
        req.getUserProperties().putAll(session.getUserProperties());
        interpolator.interpolateModel(m, null, req, null);

        return m.getDependencyManagement().getDependencies();
    }

    /**
     * Prepares {@link MetadataRequest}s to get maven-metadata.xml for proper groupId:artifactId for given product. We
     * have to consult this metadata ourselves, because single groupId:artifactId is used for all the versions of
     * supported products.
     * @param session
     * @param repositories
     * @return
     */
    private List<MetadataRequest> configurePatchMetadataRequest(MavenSession session, Dependency productBom, List<RemoteRepository> repositories) {
        List<MetadataRequest> requests = new ArrayList<>(repositories.size());
        String groupId;
        String artifactId;
        if (bomCsbArtifactId.equals(productBom.getArtifactId())) {
            groupId = mdCsbGroupId;
            artifactId = mdCsbArtifactId;

            DefaultMetadata md = new DefaultMetadata(groupId, artifactId, "maven-metadata.xml", Metadata.Nature.RELEASE);
            // local repository
            requests.add(new MetadataRequest(md, null, ""));
            // remote repositories
            for (RemoteRepository repo : repositories) {
                requests.add(new MetadataRequest(md, repo, ""));
            }
        }

        return requests;
    }

    /**
     * Finds the latest suitable version of the patch metadata to use
     * @param bom
     * @param results
     * @return
     */
    private String findLatestMetadataVersion(Dependency bom, List<MetadataResult> results) {
        ProductVersion bomVersion = new ProductVersion(bom.getVersion());

        Map<String, Versioning> metadata = new TreeMap<>();
        for (MetadataResult result : results) {
            if (result != null && result.isResolved()) {
                try (FileReader reader = new FileReader(result.getMetadata().getFile())) {
                    org.apache.maven.artifact.repository.metadata.Metadata md = new MetadataXpp3Reader().read(reader);
                    Versioning v = md.getVersioning();
                    if (v != null) {
                        // we don't care about /metadata/versioning/release, because it may be for newly deployed
                        // metadata for older version
                        metadata.put(v.getLastUpdated(), v);
                    }
                } catch (IOException | XmlPullParserException e) {
                    logger.warn("[PATCH] Problem parsing Maven Metadata {}: {}", result.getMetadata().getFile(), e.getMessage(), e);
                }
            }
        }

        Set<ComparableVersion> versions = new TreeSet<>(Comparator.reverseOrder());
        // iterate from oldest to newest metadata, where newer overwrite older versions
        for (Versioning versioning : metadata.values()) {
            for (String version : versioning.getVersions()) {
                // the problem is that "canonical" maven versions are ONLY:
                //  - major, major.minor or major.minor.build
                //  - major-qualifier, major.minor-qualifier or major.minor.build-qualifier
                // when qualifier is parsable as int, it'll become "build number", when version is unparsable,
                // everything becomes just the "qualifier", so:
                // 1-1 == 1.0.0 with build number = 1
                // 1-a == 1.0.0 with qualifier = "a"
                // 1.1.1.1 == 0.0.0 with qualifier = "1.1.1.1"
                // so for example new org.apache.maven.artifact.versioning.DefaultArtifactVersion("1.2.3.4") will
                // return:
                // DefaultArtifactVersion.getMajorVersion(): "0"
                // DefaultArtifactVersion.getQualifier(): "1.2.3.4"
                //
                // we can imagine the problems with jackson-databind 2.9.10.4-redhat-00001 or
                // fuse-springboot-bom 7.7.0.fuse-sb2-770010-redhat-00001 (actual version in MRRC/ga)
                //
                // fortunately GenericArtifactVersions uses org.apache.maven.artifact.versioning.ComparableVersion
                // when comparing, but we have to take care of checking major.minor version
                ProductVersion metadataVersion = new ProductVersion(version);
                if (bomCsbArtifactId.equals(bom.getArtifactId())) {
                    if (bomVersion.getMajor() != metadataVersion.getMajor()
                            || bomVersion.getMinor() != metadataVersion.getMinor()) {
                        logger.debug("[PATCH] Skipping metadata {}", version);
                        continue;
                    }
                }
                logger.debug("[PATCH] Found metadata {}", version);
                versions.add(new ComparableVersion(version));
            }
        }

        // simply return newest version
        return versions.size() == 0 ? null : versions.iterator().next().toString();
    }

    /**
     * Checks which BOM do we use in one of reactors projects (if at all) and prepares an {@link ArtifactRequest}
     * to fetch relevant, product-dependent metadata.
     * @param session
     * @param productBom the only valid product BOM
     * @param version
     * @return
     */
    private ArtifactRequest configurePatchArtifactRequest(MavenSession session, Dependency productBom, String version) {
        ArtifactRequest request = new ArtifactRequest();

        if (bomCsbArtifactId.equals(productBom.getArtifactId())) {
            request.setArtifact(new DefaultArtifact(String.format("%s:%s:%s:%s", mdCsbGroupId, mdCsbArtifactId,
                    mdCsbType, version)));
        }

        return request;
    }

    /**
     * Parses Patch metadata XML into {@link PatchMetadata}
     * @param patchMetadataFile
     * @return
     * @throws MavenExecutionException
     */
    private PatchMetadata readPatchMetadata(File patchMetadataFile) throws MavenExecutionException {
        PatchMetadata patch = new PatchMetadata();

        try {
            try (FileReader reader = new FileReader(patchMetadataFile)) {
                Xpp3Dom dom = Xpp3DomBuilder.build(reader);

                Xpp3Dom productDom = dom.getChild("product-bom");
                if (productDom == null) {
                    throw new IllegalStateException("Can't find <product-bom> element in patch metadata");
                }
                patch.setProductGroupId(productDom.getAttribute("groupId"));
                patch.setProductArtifactId(productDom.getAttribute("artifactId"));
                patch.setProductVersionRange(AffectedArtifactSpec.GVS.parseVersionRange(productDom.getAttribute("versions")));

                Xpp3Dom cvesWrapper = dom.getChild("cves");
                if (cvesWrapper != null) {
                    for (Xpp3Dom cveDom : cvesWrapper.getChildren("cve")) {
                        CVE cve = new CVE();
                        cve.setId(cveDom.getAttribute("id"));
                        cve.setDescription(cveDom.getAttribute("description"));
                        cve.setCveLink(cveDom.getAttribute("cve-link"));
                        cve.setBzLink(cveDom.getAttribute("bz-link"));
                        patch.getCves().add(cve);
                        for (Xpp3Dom affects : cveDom.getChildren("affects")) {
                            AffectedArtifactSpec spec = new AffectedArtifactSpec();
                            spec.setGroupIdSpec(affects.getAttribute("groupId"));
                            spec.setArtifactIdSpec(affects.getAttribute("artifactId"));
                            spec.setVersionRange(AffectedArtifactSpec.GVS.parseVersionRange(affects.getAttribute("versions")));
                            spec.setFixVersion(AffectedArtifactSpec.GVS.parseVersion(affects.getAttribute("fix")));
                            cve.getAffected().add(spec);
                        }
                    }
                }
                Xpp3Dom fixesWrapper = dom.getChild("fixes");
                if (fixesWrapper != null) {
                    for (Xpp3Dom fixDom : fixesWrapper.getChildren("fix")) {
                        Fix fix = new Fix();
                        fix.setId(fixDom.getAttribute("id"));
                        fix.setDescription(fixDom.getAttribute("description"));
                        fix.setLink(fixDom.getAttribute("link"));
                        patch.getFixes().add(fix);
                        for (Xpp3Dom affects : fixDom.getChildren("affects")) {
                            AffectedArtifactSpec spec = new AffectedArtifactSpec();
                            spec.setGroupIdSpec(affects.getAttribute("groupId"));
                            spec.setArtifactIdSpec(affects.getAttribute("artifactId"));
                            spec.setVersionRange(AffectedArtifactSpec.GVS.parseVersionRange(affects.getAttribute("versions")));
                            spec.setFixVersion(AffectedArtifactSpec.GVS.parseVersion(affects.getAttribute("fix")));
                            fix.getAffected().add(spec);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new MavenExecutionException(e.getMessage(), e);
        }

        return patch;
    }

    private void cleanupRepository(File tmpDir) {
        try {
            Files.walkFileTree(tmpDir.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    file.toFile().delete();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    dir.toFile().delete();
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            logger.warn("Problem during temporary patch repository cleanup: {}", e.getMessage(), e);
        }
    }

}
