package org.apache.camel.component.file.remote.springboot.ftp;

import org.apache.camel.component.file.remote.springboot.AbstractBaseFtp;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.nio.file.Path;


public class BaseFtp extends AbstractBaseFtp {
    protected static final String AUTH_VALUE_SSL = "SSLv3";
    protected static final String AUTH_VALUE_TLS = "TLSv1.2";

    @RegisterExtension
    static FtpEmbeddedService service = new FtpEmbeddedService();

    @Override
    protected int getPort() {
        return service.getPort();
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
