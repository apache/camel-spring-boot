package org.apache.camel.websocket.jsr356.springboot;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.websocket.server.ServerContainer;

import org.apache.camel.websocket.jsr356.JSR356WebSocketComponent;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import static java.util.Optional.ofNullable;

/**
 * Auto configuration class which sets up a ServletContextInitializer to register the websocket
 * {@link ServerContainer} with the JSR356WebSocketComponent.
 *
 * This is needed for embedded server mode, which ignores the ServletContainerInitializer provided by the camel component.
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore(JSR356WebSocketComponentAutoConfiguration.class)
public class JSR356WebSocketContextListenerAutoConfiguration {

    @Bean
    public ServletContextInitializer jsr356ServletContextInitializer() {
        return new ServletContextInitializer() {
            @Override
            public void onStartup(ServletContext servletContext) throws ServletException {
                servletContext.addListener(new ServletContextListener() {
                    @Override
                    public void contextInitialized(ServletContextEvent sce) {
                        final String contextPath = sce.getServletContext().getContextPath();
                        ofNullable(sce.getServletContext().getAttribute(ServerContainer.class.getName())).map(ServerContainer.class::cast)
                                .ifPresent(container -> JSR356WebSocketComponent.registerServer(contextPath, container));
                    }

                    @Override
                    public void contextDestroyed(ServletContextEvent sce) {
                        JSR356WebSocketComponent.unregisterServer(sce.getServletContext().getContextPath());
                    }
                });
            }
        };
    }
}
