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

import java.nio.file.Path
import java.nio.file.Paths
import org.slf4j.LoggerFactory

def log = LoggerFactory.getLogger("archetype-post-generate")

Path projectPath = Paths.get(request.outputDirectory, request.artifactId)
ProcessBuilder processBuilder = new ProcessBuilder()

def basePath = request.getRepositorySession().getSystemProperties().get("maven.multiModuleProjectDirectory");
def mvnCmd = Path.of(basePath, "mvnw").toFile().exists() ? Path.of(basePath, "mvnw").toString() : "mvn"
log.info("generating maven wrapper using {} command", mvnCmd)
processBuilder.command(mvnCmd, "wrapper:wrapper", "-Dmaven=" + request.properties["maven-version"])
processBuilder.directory(projectPath.toFile())
processBuilder.inheritIO()

try {
    Process process = processBuilder.start()
    int exitCode = process.waitFor()
    
    if (exitCode == 0) {
        log.info("Maven wrapper generated successfully")
    } else {
        log.warn("Failed to generate Maven wrapper. You can run 'mvn wrapper:wrapper' manually after resolving project dependencies.")
    }
} catch (Exception e) {
    log.warn("Could not generate Maven wrapper: ${e.message}")
}