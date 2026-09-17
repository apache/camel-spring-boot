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
package org.apache.camel.springboot.patch.extensions;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.MavenExecutionException;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.InputData;
import org.apache.maven.wagon.OutputData;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.StreamWagon;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//@Component(hint = "zip", role = Wagon.class, instantiationStrategy = "per-lookup")
public class ZipWagon extends StreamWagon {

    public static final Logger LOG = LoggerFactory.getLogger(ZipWagon.class);
    private static final DateFormat FMT = new SimpleDateFormat("yyyyMMddHHmmss");

    File base = null;

    static {
        TimeZone timezone = java.util.TimeZone.getTimeZone("UTC");
        FMT.setTimeZone(timezone);
    }

    /**
     * Unpacks a ZIP file into normal directory structure available later as normal (remote) repository.
     * @param zipFile
     * @return
     * @throws MavenExecutionException
     */
    public static File unpackPatchRepository(File zipFile) throws MavenExecutionException {
        File tmpDir = null;
        MessageDigest md5 = null;
        try {
            tmpDir = Files.createTempDirectory(zipFile.getName() + "-").toFile();
            md5 = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            throw new MavenExecutionException("Can't create temporary directory to unzip " + zipFile, e);
        }

        try (ZipFile zf = new ZipFile(zipFile)) {
            byte[] buf = new byte[16384];
            for (Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements(); ) {
                ZipEntry ze = e.nextElement();
                if (ze.isDirectory()) {
                    continue;
                }
                String name = trimName(ze.getName());
                File target = new File(tmpDir, name);
                if ("maven-metadata-local.xml".equals(target.getName())) {
                    target = new File(target.getParentFile(), "maven-metadata.xml");
                    name = name.replaceFirst("maven-metadata-local.xml$", "maven-metadata.xml");
                }
                if ("maven-metadata-local.xml.md5".equals(target.getName())) {
                    target = new File(target.getParentFile(), "maven-metadata.xml.md5");
                    name = name.replaceFirst("maven-metadata-local.xml.md5$", "maven-metadata.xml.md5");
                }
                boolean checksumFile = name.endsWith(".md5");
                if (!checksumFile) {
                    md5.reset();
                } else {
                    if (target.exists()) {
                        // let's remove the generated one
                        target.delete();
                    }
                }
                target.getParentFile().mkdirs();
                try (InputStream is = zf.getInputStream(ze)) {
                    try (OutputStream os = new FileOutputStream(target)) {
                        int read = -1;
                        while ((read = is.read(buf)) > 0) {
                            os.write(buf, 0, read);
                            if (!checksumFile) {
                                md5.update(buf, 0, read);
                            }
                        }
                    }
                }

                // even ef there may be (later) a checksum file in the ZIP, we'll generate one just in case
                File targetMd5 = new File(tmpDir, name + ".md5");
                if (!targetMd5.exists()) {
                    try (FileWriter fw = new FileWriter(targetMd5)) {
                        byte[] sum = md5.digest();
                        for (byte b : sum) {
                            fw.write(String.format("%02x", b).toUpperCase());
                        }
                    }
                    targetMd5.setLastModified(ze.getTime());
                }

                target.getParentFile().setLastModified(ze.getTime());
                target.setLastModified(ze.getTime());
            }
        } catch (IOException e) {
            throw new MavenExecutionException("Problem extracting data from " + zipFile, e);
        }

        return tmpDir;
    }

    public static String trimName(String name) {
        if (name.startsWith("system/")) {
            name = name.substring("system/".length());
        } else if (name.startsWith("repository/")) {
            name = name.substring("repository/".length());
        }
        return name;
    }

    @Override
    protected void openConnectionInternal() throws ConnectionException, AuthenticationException {
        if (getRepository() == null) {
            throw new ConnectionException("Unable to operate with a null repository.");
        }

        base = null;
        String baseDir = getRepository().getUrl();
        if (baseDir.startsWith("zip:")) {
            baseDir = baseDir.substring(4);
        }
        if (baseDir.startsWith("file:")) {
            try {
                base = new File(new URL(baseDir).toURI());
            } catch (URISyntaxException | MalformedURLException e) {
                throw new ConnectionException("Can't use " + baseDir + " repository: " + e.getMessage(), e);
            }
        } else {
            // rather strange case
            base = new File(baseDir);
        }

        fireSessionDebug("Opening connection to " + getRepository().getId());
    }

    @Override
    public void closeConnection() throws ConnectionException {
        fireSessionDebug("Closing connection to " + getRepository().getId());
    }

    @Override
    public void fillInputData(InputData inputData) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        // similar to org.apache.maven.wagon.providers.file.FileWagon.fillInputData

        Resource resource = inputData.getResource();

        String name = resource.getName();

        File file = new File(base, name);

        if ("maven-metadata.xml".equals(file.getName())) {
            // we may want to generate it if it doesn't exist
            if (!file.exists()) {
                generateMavenMetadata(name, file);
            }
        }

        if (!file.exists()) {
            throw new ResourceDoesNotExistException("File: " + file + " does not exist");
        }

        try {
            InputStream in = new BufferedInputStream(new FileInputStream(file));

            inputData.setInputStream(in);

            resource.setContentLength(file.length());

            resource.setLastModified(file.lastModified());
        } catch (FileNotFoundException e) {
            throw new TransferFailedException("Could not read from file: " + file.getAbsolutePath(), e);
        }
    }

    /**
     * Converts layout-specific name into groupId + artifactId.
     * @param name
     * @return
     */
    private String[] groupAndArtifactIds(String name) {
        String[] split = name.split("/");
        String[] result = new String[2];
        result[0] = Arrays.stream(split, 0, split.length - 2).collect(Collectors.joining("."));
        result[1] = split[split.length - 2];

        return result;
    }

    @Override
    public void fillOutputData(OutputData outputData) throws TransferFailedException {
        throw new TransferFailedException("PUT not supported for ZIP repository");
    }

    private void generateMavenMetadata(String name, File file) throws TransferFailedException {
        Metadata md = new Metadata();
        String[] ga = groupAndArtifactIds(name);
        md.setGroupId(ga[0]);
        md.setArtifactId(ga[1]);
        Versioning v = new Versioning();
        md.setVersioning(v);
        File[] versions = file.getParentFile().listFiles();

        String latestRelease = null;
        long lastUpdated = -1L;
        if (versions != null) {
            for (File fv : versions) {
                v.addVersion(fv.getName());
                long lu = fv.lastModified();
                if (lu > lastUpdated) {
                    lastUpdated = lu;
                    latestRelease = fv.getName();
                }
            }
            if (lastUpdated != -1L) {
                v.setLatest(latestRelease);
                v.setRelease(latestRelease);
                v.setLastUpdated(FMT.format(new Date(lastUpdated)));
            }
        }

        try {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                new MetadataXpp3Writer().write(baos, md);

                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(baos.toByteArray());
                }
                try {
                    MessageDigest md5 = MessageDigest.getInstance("MD5");
                    md5.update(baos.toByteArray());
                    File md5File = new File(file.getParentFile(), "maven-metadata.xml.md5");
                    try (FileWriter fw = new FileWriter(md5File)) {
                        byte[] sum = md5.digest();
                        for (byte b : sum) {
                            fw.write(String.format("%02x", b).toUpperCase());
                        }
                    }
                } catch (NoSuchAlgorithmException e) {
                    LOG.warn("Can't generate MD5 checksum for maven-metadata.xml: {}", e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new TransferFailedException("Problem generating metadata for ZIP patch repository", e);
        }
    }

}
