package org.apache.camel.component.file.remote.springboot.ftps;

import org.apache.camel.component.file.remote.springboot.AbstractBaseFtp;
import org.junit.jupiter.api.extension.RegisterExtension;


public class BaseFtpsImplicitClientAuth extends AbstractBaseFtp {
    protected static final String AUTH_VALUE_SSL = "SSLv3";
    protected static final String AUTH_VALUE_TLS = "TLSv1.2";

    @RegisterExtension
    static FtpsEmbeddedService service = new FtpsEmbeddedService(true, AUTH_VALUE_TLS, true);

    @Override
    protected int getPort() {
        return service.getPort();
    }
}
