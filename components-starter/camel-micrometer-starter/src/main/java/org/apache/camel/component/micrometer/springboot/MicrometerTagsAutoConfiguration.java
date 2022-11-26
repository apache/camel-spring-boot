package org.apache.camel.component.micrometer.springboot;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.boot.util.ConditionalOnCamelContextAndAutoConfigurationBeans;
import org.springframework.boot.actuate.metrics.web.servlet.DefaultWebMvcTagsProvider;
import org.springframework.boot.actuate.metrics.web.servlet.WebMvcTagsProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Configuration(proxyBeanMethods = false)
@Conditional(ConditionalOnCamelContextAndAutoConfigurationBeans.class)
@AutoConfigureAfter({CamelAutoConfiguration.class})
public class MicrometerTagsAutoConfiguration {

    /**
     * To integrate with micrometer to include uri in tags when for example using
     * camel rest-dsl with servlet.
     */
    @Bean
    WebMvcTagsProvider webMvcTagsProvider() {
        return new DefaultWebMvcTagsProvider() {
            @Override
            public Iterable<Tag> getTags(HttpServletRequest request, HttpServletResponse response,
                                         Object handler, Throwable exception) {
                String uri = request.getServletPath();
                if (uri == null || uri.isEmpty()) {
                    uri = request.getPathInfo();
                } else {
                    String p = request.getPathInfo();
                    if (p != null) {
                        uri = uri + p;
                    }
                }
                return Tags.concat(
                        super.getTags(request, response, handler, exception),
                        Tags.of(Tag.of("uri", uri))
                );
            }
        };
    }
}
