/*
 * Copyright 2005-2023 Red Hat, Inc.
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
package org.apache.camel.springboot.patch;

import java.io.File;
import java.util.Properties;
import java.util.ServiceLoader;

import com.google.inject.AbstractModule;
import org.apache.commons.io.FileUtils;
import org.apache.maven.Maven;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.internal.aether.DefaultRepositorySystemSessionFactory;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.interpolation.DefaultModelVersionProcessor;
import org.apache.maven.model.interpolation.StringVisitorModelInterpolator;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.repository.legacy.LegacyRepositorySystem;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.wagon.WagonProvider;
import org.eclipse.aether.transport.wagon.WagonTransporterFactory;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionRange;
import org.apache.camel.springboot.patch.extensions.ZipWagon;
import org.apache.camel.springboot.patch.model.ProductVersion;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestEngine;

import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MavenTest {

    @Test
    public void mavenComponentsUsingPlexus() throws Exception {
        // org.apache.maven.cli.MavenCli.doMain(org.apache.maven.cli.CliRequest)

        ClassWorld world = new ClassWorld("plexus.core", Thread.currentThread().getContextClassLoader());
        ClassRealm realm = world.getRealm("plexus.core");

        realm.setParentClassLoader(MavenTest.class.getClassLoader());

        ContainerConfiguration cc = new DefaultContainerConfiguration()
                .setClassWorld(world)
                .setRealm(realm)
                .setClassPathScanning(PlexusConstants.SCANNING_INDEX)
                .setAutoWiring(true)
                .setJSR250Lifecycle(true)
                .setName("maven");

        PlexusContainer container = new DefaultPlexusContainer(cc, new AbstractModule() {
            @Override
            protected void configure() {
                bind(ILoggerFactory.class).toInstance(LoggerFactory.getILoggerFactory());
            }
        });

        Maven maven = container.lookup(Maven.class);
        System.out.println(maven);

        DefaultRepositorySystemSessionFactory sf = container.lookup(DefaultRepositorySystemSessionFactory.class);
        System.out.println(sf);

        // (file:/data/ggrzybek/.m2/repository/org/apache/maven/maven-compat/3.6.3/maven-compat-3.6.3.jar <no signer certificates>)
        System.out.println(container.lookup(org.apache.maven.repository.RepositorySystem.class));
        // (file:/data/ggrzybek/.m2/repository/org/apache/maven/resolver/maven-resolver-impl/1.4.1/maven-resolver-impl-1.4.1.jar <no signer certificates>)
        System.out.println(container.lookup(org.eclipse.aether.RepositorySystem.class));
    }

    @Test
    public void mavenComponentsUsingLocator() throws Exception {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.setService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.setService(org.apache.maven.repository.RepositorySystem.class, LegacyRepositorySystem.class);
//        locator.setService(org.eclipse.aether.RepositorySystem.class, DefaultRepositorySystem.class);

        org.apache.maven.repository.RepositorySystem mavenSystem
                = locator.getService(org.apache.maven.repository.RepositorySystem.class);
        System.out.println(mavenSystem);
        // this can't be used this way, because it's annotation driven and dependencies are not injected
        // using org.eclipse.aether.spi.locator.Service.initService()
//        mavenSystem.resolve(new ArtifactResolutionRequest());

        org.eclipse.aether.RepositorySystem resolverSystem
                = locator.getService(org.eclipse.aether.RepositorySystem.class);
        System.out.println(resolverSystem);

        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        LocalRepository localRepository = new LocalRepository(new File(System.getProperty("user.home"), ".m2/repository"));
        session.setLocalRepositoryManager(resolverSystem.newLocalRepositoryManager(session, localRepository));

        ArtifactRequest request = new ArtifactRequest();
        String junitVersion = ServiceLoader.load(TestEngine.class).findFirst().get().getVersion().get();
        request.setArtifact(new DefaultArtifact("org.junit.jupiter:junit-jupiter-engine:" + junitVersion));
        ArtifactResult result = resolverSystem.resolveArtifact(session, request);
        System.out.println(result.getArtifact().getFile());
    }

    @Test
    public void embeddedModelInterpolation() {
        StringVisitorModelInterpolator interpolator = new StringVisitorModelInterpolator();

        Model model = new Model();

        Dependency dependency = new Dependency();
        dependency.setGroupId("g");
        dependency.setArtifactId("${my.artifactId}");
        dependency.setVersion("${my.version}");
        dependency.setScope("import");
        dependency.setType("pom");
        model.setDependencyManagement(new DependencyManagement());
        model.getDependencyManagement().addDependency(dependency);

        Properties properties = new Properties();
        properties.setProperty("my.version", "1.0");
        model.setProperties(properties);

        System.setProperty("my.artifactId", "my-bom");
        ModelBuildingRequest req = new DefaultModelBuildingRequest();
        req.setSystemProperties(System.getProperties());
        interpolator.setVersionPropertiesProcessor(new DefaultModelVersionProcessor());
        interpolator.interpolateModel(model, null, req, null);

        assertEquals("1.0", model.getDependencyManagement().getDependencies().get(0).getVersion());
        assertEquals("my-bom", model.getDependencyManagement().getDependencies().get(0).getArtifactId());
    }

    @Test
    public void versionParsing() throws Exception {
        String[] versions = new String[] {
                "1", "1.2", "1.2.3", "1.2.3.4", "1.2.3.4.5",
                "1-1", "1.2-1", "1.2.3-1", "1.2.3.4-1", "1.2.3.4.5-1",
                "1-a", "1.2-a", "1.2.3-a", "1.2.3.4-a", "1.2.3.4.5-a",
                "1.redhat", "1.2.redhat", "1.2.3.redhat", "1.2.3.4.redhat", "1.2.3.4.5.redhat",
                "1.redhat-1", "1.2.redhat-1", "1.2.3.redhat-1", "1.2.3.4.redhat-1", "1.2.3.4.5.redhat-1",
                "1-redhat", "1.2-redhat", "1.2.3-redhat", "1.2.3.4-redhat", "1.2.3.4.5-redhat",
                "1-redhat-1", "1.2-redhat-1", "1.2.3-redhat-1", "1.2.3.4-redhat-1", "1.2.3.4.5-redhat-1",
                "1.csb-3_20_0-redhat-00001", "1.2.csb-3_20_0-redhat-00001", "1.2.3.csb-3_20_0-redhat-00001",
                "1.2.3.4.csb-3_20_0-redhat-00001", "1.2.3.4.5.csb-3_20_0-redhat-00001",
                "1-csb-3_20_0-redhat-00001", "1.2-csb-3_20_0-redhat-00001", "1.2.3-csb-3_20_0-redhat-00001",
                "1.2.3.4-csb-3_20_0-redhat-00001", "1.2.3.4.5-csb-3_20_0-redhat-00001",
        };
        System.out.println("Maven:");
        for (String v : versions) {
            ArtifactVersion version = new DefaultArtifactVersion(v);
            if (1 == version.getMajorVersion()) {
                System.out.printf(" - %s -> [%s].[%s].[%s] number: [%s] qualifier: [%s]%n", v,
                        version.getMajorVersion(), version.getMinorVersion(),
                        version.getIncrementalVersion(), version.getBuildNumber(),
                        version.getQualifier());
            } else {
                System.out.printf(" ! %s -> [%s].[%s].[%s] number: [%s] qualifier: [%s]%n", v,
                        version.getMajorVersion(), version.getMinorVersion(),
                        version.getIncrementalVersion(), version.getBuildNumber(),
                        version.getQualifier());
            }
        }

        System.out.println("Aether:");
        GenericVersionScheme gvs = new GenericVersionScheme();
        for (String v : versions) {
            Version version = gvs.parseVersion(v);
            System.out.printf(" - %s -> %s%n", v, version);
        }

        System.out.println("Comparable:");
        for (String v : versions) {
            ComparableVersion version = new ComparableVersion(v);
            System.out.printf(" - %s -> %s%n", v, version.getCanonical());
        }
    }

    @Test
    public void comparingVersions() throws Exception {
        GenericVersionScheme gvs = new GenericVersionScheme();
        //assertTrue(gvs.parseVersion("2.9.10.4-redhat-00001").compareTo(gvs.parseVersion("10.0.0.1-redhat-00001")) < 0);
        assertTrue(new DefaultArtifactVersion("2.9.10.4-redhat-00001").compareTo(new DefaultArtifactVersion("10.0.0.1-redhat-00001")) < 0);
    }

    @Test
    public void productVersions() {
        assertEquals(1, new ProductVersion("1").getMajor());
        assertEquals(1, new ProductVersion("1.1").getMajor());
        assertEquals(1, new ProductVersion("1.1.1").getMajor());
        assertEquals(1, new ProductVersion("1.1.1.1").getMajor());
        assertEquals(1, new ProductVersion("1.1.1.1.1").getMajor());
        assertEquals(1, new ProductVersion("1.redhat").getMajor());
        assertEquals(1, new ProductVersion("1.1.redhat").getMajor());
        assertEquals(1, new ProductVersion("1.1.1.redhat").getMajor());
        assertEquals(1, new ProductVersion("1.1.1.1.redhat").getMajor());
        assertEquals(1, new ProductVersion("1.1.1.1.1.redhat").getMajor());
        assertEquals(0, new ProductVersion("1-redhat").getMajor());
        assertEquals(1, new ProductVersion("1.1-redhat").getMajor());
        assertEquals(1, new ProductVersion("1.1.1-redhat").getMajor());
        assertEquals(1, new ProductVersion("1.1.1.1-redhat").getMajor());
        assertEquals(1, new ProductVersion("1.1.1.1.1-redhat").getMajor());
        assertEquals("", new ProductVersion("1").getQualifier());
        assertEquals("", new ProductVersion("1.1").getQualifier());
        assertEquals("", new ProductVersion("1.1.1").getQualifier());
        assertEquals("", new ProductVersion("1.1.1.1").getQualifier());
        assertEquals("", new ProductVersion("1.1.1.1.1").getQualifier());
        assertEquals("redhat", new ProductVersion("1.redhat").getQualifier());
        assertEquals("redhat", new ProductVersion("1.1.redhat").getQualifier());
        assertEquals("redhat", new ProductVersion("1.1.1.redhat").getQualifier());
        assertEquals("redhat", new ProductVersion("1.1.1.1.redhat").getQualifier());
        assertEquals("redhat", new ProductVersion("1.1.1.1.1.redhat").getQualifier());
        assertEquals("1-redhat", new ProductVersion("1-redhat").getQualifier());
        assertEquals("1-redhat", new ProductVersion("1.1-redhat").getQualifier());
        assertEquals("1-redhat", new ProductVersion("1.1.1-redhat").getQualifier());
        assertEquals("1-redhat", new ProductVersion("1.1.1.1-redhat").getQualifier());
        assertEquals("1-redhat", new ProductVersion("1.1.1.1.1-redhat").getQualifier());
    }

    @Test
    public void matchingVersions() throws Exception {
        GenericVersionScheme gvs = new GenericVersionScheme();
        VersionRange range = gvs.parseVersionRange("[3.20, 3.21.1)");
        assertTrue(range.containsVersion(gvs.parseVersion("3.20.0")));
        assertTrue(range.containsVersion(gvs.parseVersion("3.20.0.redhat-00001")));
        assertTrue(range.containsVersion(gvs.parseVersion("3.20.1.redhat-00001")));
        assertTrue(range.containsVersion(gvs.parseVersion("3.21")));
        assertTrue(range.containsVersion(gvs.parseVersion("3.21.0")));
        assertTrue(range.containsVersion(gvs.parseVersion("3.21.0.1")));
        assertFalse(range.containsVersion(gvs.parseVersion("3.21.1")));
        assertTrue(range.containsVersion(gvs.parseVersion("3.21.redhat-00001")));
        assertTrue(range.containsVersion(gvs.parseVersion("3.21.0.redhat-00001")));
        assertFalse(range.containsVersion(gvs.parseVersion("3.21.1.redhat-00001")));
    }

}
