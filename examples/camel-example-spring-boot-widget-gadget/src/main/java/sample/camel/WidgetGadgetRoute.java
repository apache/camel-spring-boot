package sample.camel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WidgetGadgetRoute extends RouteBuilder {

    @Autowired
    JmsConnectionFactory amqpConnectionFactory;

    @Override
    public void configure() throws Exception {
        from("amqp:queue:order.queue")
                .choice()
                    .when().jsonpath("$.order[?(@.product=='widget')]")
                        .to("log:widget")
                        .to("amqp:queue:widget.queue")
                    .otherwise()
                        .to("log:gadget")
                        .to("amqp:queue:gadget.queue");
    }

}
