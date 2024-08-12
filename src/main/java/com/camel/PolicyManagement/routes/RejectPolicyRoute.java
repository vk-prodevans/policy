package com.camel.PolicyManagement.routes;

import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.sql.SqlComponent;
import org.apache.camel.model.rest.RestBindingMode;

public class RejectPolicyRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        getContext().getComponent("sql", SqlComponent.class);

        restConfiguration().component("jetty")
                .port(8084).host("0.0.0.0")
                .bindingMode(RestBindingMode.json);

        // Handle CORS preflight requests
        from("rest:options:rejectPolicies")
            .setHeader("Access-Control-Allow-Origin", constant("*"))
            .setHeader("Access-Control-Allow-Methods", constant("POST, GET, OPTIONS, DELETE, PUT"))
            .setHeader("Access-Control-Max-Age", constant("3600"))
            .setHeader("Access-Control-Allow-Headers", constant("Origin, X-Requested-With, Content-Type, Accept"))
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(204))
            .setBody(constant(""))
            .end();

        // Actual POST request to reject policies
        from("rest:post:rejectPolicies")
            .setHeader("Access-Control-Allow-Origin", constant("*"))
            .setHeader("Access-Control-Allow-Headers", constant("Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers"))
            .choice()
                .when(header(Exchange.HTTP_METHOD).isEqualTo("OPTIONS"))
                    .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(204))
                    .setBody(constant(""))
                .endChoice()
                .otherwise()
                    .routeId("rejectPolicies")
                    .log("rejectPolicies route started at: ${date:now:yyyy-MM-dd HH:mm:ss}")
                    .doTry()
                        .unmarshal().json()
                        .process(exchange -> {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> requestBody = exchange.getIn().getBody(Map.class);
                            String insurance_id = requestBody.get("insurance_id").toString();
                            exchange.getIn().setHeader("insurance_id", insurance_id);
                        })
                        .log("Headers before choice: ${headers}")
                        .setBody(simple("UPDATE application_table SET application_status = 'Rejected' WHERE insurance_id = ${header.insurance_id}"))
                        .log("Executing SQL query: ${body}")
                        .to("jdbc:datasource")
                    .endDoTry()
                    .setHeader("Access-Control-Allow-Origin", constant("*"))
                    .setHeader("Access-Control-Allow-Methods", constant("POST, GET, OPTIONS, DELETE, PUT"))
                    .setHeader("Access-Control-Allow-Headers", constant("Origin, X-Requested-With, Content-Type, Accept"))
                .endChoice()
            .end();
    }
}
