package org.apache.camel.component.file.remote.springboot.sftp;

import org.apache.camel.component.file.remote.springboot.AbstractBaseFtp;
import org.apache.camel.component.file.remote.springboot.ftp.FtpEmbeddedService;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.nio.file.Path;


public class BaseSftp extends AbstractBaseFtp {

    @RegisterExtension
    static SftpEmbeddedService service = new SftpEmbeddedService();

    @Override
    protected int getPort() {
        return service.getPort();
    }

    protected String getRootDir() {
        return service.getFtpRootDir().toString();
    }

    protected Path ftpFile(String file) {
        return service.getFtpRootDir().resolve(file);
    }

    protected String getFtpUrl(String user, String password) {
        StringBuilder url = new StringBuilder("ftp://");
        url.append(user == null ? "" : user + "@");
        url.append("localhost:" + service.getPort() + "/");
        url.append(password == null ? "" : "?password=" + password);
        return url.toString();
    }

}
