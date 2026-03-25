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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.io.IOUtils;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Generate BOM with only the camel spring boot starter dependencies.
 */
@Mojo(name = "generate-bom", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true)
public class BomGeneratorMojo extends AbstractMojo {

    /**
     * The maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    /**
     * The source pom template file.
     */
    @Parameter(defaultValue = "${basedir}/pom.xml")
    protected File sourcePom;

    /**
     * The pom file.
     */
    @Parameter(defaultValue = "${project.build.directory}/${project.name}-pom.xml")
    protected File targetPom;

    @Parameter(defaultValue = "${basedir}/../../components-starter")
    protected File startersDir;

    @Parameter(defaultValue = "${basedir}/../../product/src/main/resources/required-productized-camel-artifacts.txt")
    protected File requiredProductizedCamelSpringBootArtifactsFile;

    @Parameter(property = "bom.camelCommunityVersion", defaultValue = "${camel-spring-boot-community.version}")
    protected String camelCommunityVersion;

    @Parameter(property = "bom.narayanaSpringBoootVersion", defaultValue = "${narayana-spring-boot.version}")
    protected String narayanaSpringBootVersion;

    @Parameter(property = "bom.camelVersion", defaultValue = "${camel-version}")
    protected String camelVersion;

    @Component
    protected RepositorySystem repositorySystem;

    @Parameter(defaultValue="${repositorySystemSession}", readonly = true, required = true)
    protected RepositorySystemSession repoSession;

    @Parameter(property = "project.remoteArtifactRepositories", readonly = true, required = true)
    protected List<ArtifactRepository> remoteRepositories;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            List<Dependency> starters = starters();
            Document pom = loadBasePom();
            // transform
            overwriteDependencyManagement(pom, starters);
            writePom(pom);
        } catch (Exception ex) {
            throw new MojoExecutionException("Cannot generate the output BOM file", ex);
        }
    }

    private List<Dependency> starters() throws IOException {
        List<Dependency> outDependencies = new ArrayList<>();

        HashMap<String, Boolean> productizedArtifacts = RequiredProductizedArtifactsReader.getProductizedCSBArtifacts(requiredProductizedCamelSpringBootArtifactsFile);

        for (String s : productizedArtifacts.keySet()) {
            System.out.println("Starter [" + s  + "] " + productizedArtifacts.get(s));
        }

        Files.list(startersDir.toPath()).filter(Files::isDirectory)
                // must have a pom.xml to be active
                .filter(d -> {
                    File pom = new File(d.toFile(), "pom.xml");
                    return pom.isFile() && pom.exists();
                }).map(dir -> {
                    Dependency dep = new Dependency();
                    dep.setGroupId("org.apache.camel.springboot");
                    dep.setArtifactId(dir.getFileName().toString());
                    dep.setVersion(productizedArtifacts.containsKey(dir.getFileName().toString()) ? "${project.version}" : camelCommunityVersion);
                    return dep;
                }).forEach(outDependencies::add);

        // include core starters
        Dependency dep = new Dependency();
        dep.setGroupId("org.apache.camel.springboot");
        dep.setArtifactId("camel-spring-boot-starter");
        dep.setVersion(productizedArtifacts.containsKey("camel-spring-boot-starter") ? "${project.version}" : camelCommunityVersion);
        outDependencies.add(dep);
        dep = new Dependency();
        dep.setGroupId("org.apache.camel.springboot");
        dep.setArtifactId("camel-spring-boot-engine-starter");
        dep.setVersion(productizedArtifacts.containsKey("camel-spring-boot-engine-starter") ? "${project.version}" : camelCommunityVersion);
        outDependencies.add(dep);
        dep = new Dependency();
        dep.setGroupId("org.apache.camel.springboot");
        dep.setArtifactId("camel-spring-boot-xml-starter");
        dep.setVersion(productizedArtifacts.containsKey("camel-spring-boot-xml-starter") ? "${project.version}" : camelCommunityVersion);
        outDependencies.add(dep);

        // include base jars
        dep = new Dependency();
        dep.setGroupId("org.apache.camel.springboot");
        dep.setArtifactId("camel-spring-boot-xml");
        dep.setVersion("${project.version}");
        outDependencies.add(dep);
        dep = new Dependency();
        dep.setGroupId("org.apache.camel.springboot");
        dep.setArtifactId("camel-spring-boot");
        dep.setVersion("${project.version}");
        // include maven plugin
        dep = new Dependency();
        dep.setGroupId("org.apache.camel.springboot");
        dep.setArtifactId("camel-spring-boot-generator-maven-plugin");
        dep.setVersion("${project.version}");
        outDependencies.add(dep);

        // include dsl starters
        dep = new Dependency();
        dep.setGroupId("org.apache.camel.springboot");
        dep.setArtifactId("camel-cli-connector-starter");
        dep.setVersion(productizedArtifacts.containsKey("camel-cli-connector-starter") ? "${project.version}" : camelCommunityVersion);
        outDependencies.add(dep);
        dep = new Dependency();
        dep.setGroupId("org.apache.camel.springboot");
        dep.setArtifactId("camel-cli-debug-starter");
        dep.setVersion(productizedArtifacts.containsKey("camel-cli-debug-starter") ? "${project.version}" : camelCommunityVersion);
        outDependencies.add(dep);
        dep = new Dependency();
        dep.setGroupId("org.apache.camel.springboot");
        dep.setArtifactId("camel-componentdsl-starter");
        dep.setVersion(productizedArtifacts.containsKey("camel-componentdsl-starter") ? "${project.version}" : camelCommunityVersion);
        outDependencies.add(dep);
        dep = new Dependency();
        dep.setGroupId("org.apache.camel.springboot");
        dep.setArtifactId("camel-dsl-modeline-starter");
        dep.setVersion(productizedArtifacts.containsKey("camel-dsl-modeline-starter") ? "${project.version}" : camelCommunityVersion);
        outDependencies.add(dep);
        dep = new Dependency();
        dep.setGroupId("org.apache.camel.springboot");
        dep.setArtifactId("camel-endpointdsl-starter");
        dep.setVersion(productizedArtifacts.containsKey("camel-endpointdsl-starter") ? "${project.version}" : camelCommunityVersion);
        outDependencies.add(dep);
        dep = new Dependency();
        dep.setGroupId("org.apache.camel.springboot");
        dep.setArtifactId("camel-java-joor-dsl-starter");
        dep.setVersion(productizedArtifacts.containsKey("camel-java-joor-dsl-starter") ? "${project.version}" : camelCommunityVersion);
        outDependencies.add(dep);
        dep = new Dependency();
        dep.setGroupId("org.apache.camel.springboot");
        dep.setArtifactId("camel-xml-io-dsl-starter");
        dep.setVersion(productizedArtifacts.containsKey("camel-xml-io-dsl-starter") ? "${project.version}" : camelCommunityVersion);
        outDependencies.add(dep);
        dep = new Dependency();
        dep.setGroupId("org.apache.camel.springboot");
        dep.setArtifactId("camel-xml-jaxb-dsl-starter");
        dep.setVersion(productizedArtifacts.containsKey("camel-xml-jaxb-dsl-starter") ? "${project.version}" : camelCommunityVersion);
        outDependencies.add(dep);
        dep = new Dependency();
        dep.setGroupId("org.apache.camel.springboot");
        dep.setArtifactId("camel-yaml-io-starter");
        dep.setVersion(productizedArtifacts.containsKey("camel-yaml-io-starter") ? "${project.version}" : camelCommunityVersion);
        outDependencies.add(dep);
        dep = new Dependency();
        dep.setGroupId("org.apache.camel.springboot");
        dep.setArtifactId("camel-yaml-dsl-starter");
        dep.setVersion(productizedArtifacts.containsKey("camel-yaml-dsl-starter") ? "${project.version}" : camelCommunityVersion);
        outDependencies.add(dep);

        dep = new Dependency();
        dep.setGroupId("org.fusesource");
        dep.setArtifactId("camel-sap-starter");
        dep.setVersion("${project.version}");
        outDependencies.add(dep);
        dep = new Dependency();
        dep.setGroupId("org.fusesource");
        dep.setArtifactId("camel-cics-starter");
        dep.setVersion("${project.version}");
        outDependencies.add(dep);

        // Add Narayana starter and associated artifacts
        dep = new Dependency();
        dep.setGroupId("dev.snowdrop");
        dep.setArtifactId("narayana-spring-boot-starter");
        dep.setVersion(narayanaSpringBootVersion);
        outDependencies.add(dep);

        dep = new Dependency();
        dep.setGroupId("dev.snowdrop");
        dep.setArtifactId("narayana-spring-boot-core");
        dep.setVersion(narayanaSpringBootVersion);
        outDependencies.add(dep);

        dep = new Dependency();
        dep.setGroupId("dev.snowdrop");
        dep.setArtifactId("narayana-spring-boot-recovery-controller");
        dep.setVersion(narayanaSpringBootVersion);
        outDependencies.add(dep);

        // Add Agroal using the same version used in Narayana tests
        String agroalVersion = resolveAgroalVersion();

        dep = new Dependency();
        dep.setGroupId("io.agroal");
        dep.setArtifactId("agroal-spring-boot-starter");
        dep.setVersion(agroalVersion);
        outDependencies.add(dep);

        dep = new Dependency();
        dep.setGroupId("io.agroal");
        dep.setArtifactId("agroal-api");
        dep.setVersion(agroalVersion);
        outDependencies.add(dep);

        dep = new Dependency();
        dep.setGroupId("io.agroal");
        dep.setArtifactId("agroal-pool");
        dep.setVersion(agroalVersion);
        outDependencies.add(dep);

        outDependencies.sort(Comparator.comparing(d -> (d.getGroupId() + ":" + d.getArtifactId())));

        // include some dependencies for testing and management
        dep = new Dependency();
        dep.setGroupId("org.apache.camel");
        dep.setArtifactId("camel-management");
        dep.setVersion("${camel-version}");
        outDependencies.add(dep);
        dep = new Dependency();
        dep.setGroupId("org.apache.camel");
        dep.setArtifactId("camel-test-spring-junit6");
        dep.setVersion("${camel-version}");
        outDependencies.add(dep);

        return outDependencies;
    }

    /**
     * Retrieves the Map representing the properties of a given artifact
     */
    private Map<String, String> resolveArtifactProperties(String projectGroup, String projectArtifactId, String extension, String projectVersion) {
        DefaultArtifact artifact = new DefaultArtifact(projectGroup, projectArtifactId, extension,
                projectVersion);
        ArtifactRequest artifactRequest = new ArtifactRequest();
        artifactRequest.setArtifact(artifact);
        artifactRequest.setRepositories(RepositoryUtils.toRepos(remoteRepositories));
        try {
            ArtifactResult artifactResult = repositorySystem
                    .resolveArtifact(repoSession, artifactRequest);
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(new FileReader(artifactResult.getArtifact().getFile()));
            return model.getProperties().entrySet().stream().collect(Collectors.toMap(
                    e -> (String) e.getKey(),
                    e -> (String) e.getValue()
            ));
        } catch (Exception e) {
            throw new RuntimeException(String.format("unable to resolve properties in %s:$s:%s", projectGroup, projectArtifactId, projectVersion), e);
        }
    }

    private String resolveAgroalVersion() {
        return resolveArtifactProperties("dev.snowdrop", "narayana-spring-boot-parent", "pom",
                narayanaSpringBootVersion).entrySet().stream().filter(entry -> "agroal.version".equals(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No agroal.version property found in narayana-spring-boot-parent"));
    }

    private Document loadBasePom() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = dbf.newDocumentBuilder();
        Document pom = builder.parse(sourcePom);

        XPath xpath = XPathFactory.newInstance().newXPath();

        XPathExpression parentVersion = xpath.compile("/project/parent/version");
        setActualVersion(pom, parentVersion);

        XPathExpression projectVersion = xpath.compile("/project/version");
        setActualVersion(pom, projectVersion);

        return pom;
    }

    private void setActualVersion(Document pom, XPathExpression path) throws XPathExpressionException {
        Node node = (Node) path.evaluate(pom, XPathConstants.NODE);
        if (node != null && node.getTextContent() != null
                && node.getTextContent().trim().equals("${project.version}")) {
            node.setTextContent(project.getVersion());
        }
    }

    private void writePom(Document pom) throws Exception {
        XPathExpression xpath = XPathFactory.newInstance().newXPath().compile("//text()[normalize-space(.) = '']");
        NodeList emptyNodes = (NodeList) xpath.evaluate(pom, XPathConstants.NODESET);

        // Remove empty text nodes
        for (int i = 0; i < emptyNodes.getLength(); i++) {
            Node emptyNode = emptyNodes.item(i);
            emptyNode.getParentNode().removeChild(emptyNode);
        }

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        DOMSource source = new DOMSource(pom);

        targetPom.getParentFile().mkdirs();

        String content;
        try (StringWriter out = new StringWriter()) {
            StreamResult result = new StreamResult(out);
            transformer.transform(source, result);
            content = out.toString();
        }

        // Fix header formatting problem
        content = content.replaceFirst("-->", "-->\n");
        writeFileIfChanged(content, targetPom);
    }

    private void writeFileIfChanged(String content, File file) throws IOException {
        boolean write = true;

        if (file.exists()) {
            try (FileReader fr = new FileReader(file)) {
                String oldContent = IOUtils.toString(fr);
                if (!content.equals(oldContent)) {
                    getLog().info("File: " + file.getAbsolutePath() + " is updated");
                    fr.close();
                } else {
                    getLog().info("File " + file.getAbsolutePath() + " is not changed");
                    write = false;
                }
            }
        } else {
            File parent = file.getParentFile();
            parent.mkdirs();
        }

        if (write) {
            try (FileWriter fw = new FileWriter(file)) {
                IOUtils.write(content, fw);
            }
        }
    }

    private void overwriteDependencyManagement(Document pom, List<Dependency> dependencies) throws Exception {
        XPath xpath = XPathFactory.newInstance().newXPath();
        XPathExpression expr = xpath.compile("/project/dependencyManagement/dependencies");

        NodeList nodes = (NodeList) expr.evaluate(pom, XPathConstants.NODESET);
        if (nodes.getLength() == 0) {
            throw new IllegalStateException(
                    "No dependencies found in the dependencyManagement section of the current pom");
        }

        Node dependenciesSection = nodes.item(0);
        // cleanup the dependency management section
        while (dependenciesSection.hasChildNodes()) {
            Node child = dependenciesSection.getFirstChild();
            dependenciesSection.removeChild(child);
        }

        for (Dependency dep : dependencies) {

            if ("target".equals(dep.getArtifactId())) {
                // skip invalid artifact that somehow gets included
                continue;
            }

            Element dependencyEl = pom.createElement("dependency");

            Element groupIdEl = pom.createElement("groupId");
            groupIdEl.setTextContent(dep.getGroupId());
            dependencyEl.appendChild(groupIdEl);

            Element artifactIdEl = pom.createElement("artifactId");
            artifactIdEl.setTextContent(dep.getArtifactId());
            dependencyEl.appendChild(artifactIdEl);

            Element versionEl = pom.createElement("version");
            versionEl.setTextContent(dep.getVersion());
            dependencyEl.appendChild(versionEl);

            if (!"jar".equals(dep.getType())) {
                Element typeEl = pom.createElement("type");
                typeEl.setTextContent(dep.getType());
                dependencyEl.appendChild(typeEl);
            }

            if (dep.getClassifier() != null) {
                Element classifierEl = pom.createElement("classifier");
                classifierEl.setTextContent(dep.getClassifier());
                dependencyEl.appendChild(classifierEl);
            }

            if (dep.getScope() != null && !"compile".equals(dep.getScope())) {
                Element scopeEl = pom.createElement("scope");
                scopeEl.setTextContent(dep.getScope());
                dependencyEl.appendChild(scopeEl);
            }

            if (dep.getExclusions() != null && !dep.getExclusions().isEmpty()) {
                Element exclsEl = pom.createElement("exclusions");
                for (Exclusion e : dep.getExclusions()) {
                    Element exclEl = pom.createElement("exclusion");

                    Element groupIdExEl = pom.createElement("groupId");
                    groupIdExEl.setTextContent(e.getGroupId());
                    exclEl.appendChild(groupIdExEl);

                    Element artifactIdExEl = pom.createElement("artifactId");
                    artifactIdExEl.setTextContent(e.getArtifactId());
                    exclEl.appendChild(artifactIdExEl);

                    exclsEl.appendChild(exclEl);
                }
                dependencyEl.appendChild(exclsEl);
            }

            dependenciesSection.appendChild(dependencyEl);
        }
    }

    /**
     * Finds dependencies from a project.
     * E.g.: You may use this to find out witch version of <b>io.agroal:agroal-pool</b> is used by <b>me.snowdrop:narayana-spring-boot-starter-it:3.2.0</b>
     */
    private List<ArtifactResult> resolveProjectDependencies(String projectGroup, String projectArtifactId, String projectVersion, Predicate<Artifact> dependenciesFilter) {
        String artifactStr = String.format("%s:%s:%s", projectGroup, projectArtifactId, projectVersion);
        org.eclipse.aether.artifact.Artifact artifact = new DefaultArtifact(artifactStr);

        ArtifactDescriptorRequest descriptorRequest = new ArtifactDescriptorRequest();
        descriptorRequest.setArtifact(artifact);
        descriptorRequest.setRepositories(RepositoryUtils.toRepos(remoteRepositories));
        ArtifactDescriptorResult descriptorResult = null;
        try {
            descriptorResult = repositorySystem.readArtifactDescriptor(repoSession, descriptorRequest);
        } catch (ArtifactDescriptorException e) {
            throw new RuntimeException("Failed to read artifact descriptor", e);
        }

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRootArtifact(descriptorResult.getArtifact());
        collectRequest.setDependencies(descriptorResult.getDependencies());
        collectRequest.setManagedDependencies(descriptorResult.getManagedDependencies());
        collectRequest.setRepositories(descriptorRequest.getRepositories());

        DependencyRequest dependencyRequest = new DependencyRequest(collectRequest,
            (node, parents) -> dependenciesFilter.test(RepositoryUtils.toArtifact(node.getArtifact()))
        );

        try {
            return repositorySystem.resolveDependencies(repoSession, dependencyRequest).getArtifactResults();
        } catch (DependencyResolutionException e) {
            throw new RuntimeException("Failed to resolve dependencies", e);
        }
    }

}
