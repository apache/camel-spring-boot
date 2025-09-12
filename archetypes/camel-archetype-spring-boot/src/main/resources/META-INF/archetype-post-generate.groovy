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