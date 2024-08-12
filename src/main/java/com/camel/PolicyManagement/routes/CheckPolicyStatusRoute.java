package com.camel.PolicyManagement.routes;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.sql.SqlComponent;
import org.apache.camel.model.rest.RestBindingMode;

import java.sql.SQLException;

public class CheckPolicyStatusRoute extends RouteBuilder {
    @Override
    public void configure() throws Exception {

        getContext().getComponent("sql", SqlComponent.class);

        restConfiguration().component("jetty")
                .port(8084).host("0.0.0.0")
                .bindingMode(RestBindingMode.json);

        from("rest:get:checkPolicyStatus/{insuranceId}")
                .routeId("checkPolicyStatus")
                .log("CheckPolicyStatus route started at: ${date:now:yyyy-MM-dd HH:mm:ss}")
                .doTry()
                .process(exchange -> {
                    String insuranceId = exchange.getIn().getHeader("insuranceId", String.class);
                    exchange.getIn().setHeader("insuranceId", insuranceId);
                })
                .setBody(simple(
                        "SELECT i.*, a.* FROM insurance_table i LEFT JOIN application_table a ON i.insurance_id = a.insurance_id WHERE i.insurance_id = ${header.insuranceId}"))
                .to("jdbc:datasource")
                .choice()
                .when(simple("${body} == null || ${body.isEmpty()}"))
                .throwException(new SQLException("Insurance ID not found"))
                .otherwise()
                .marshal().json()
                .process(exchange -> {
                    String body = exchange.getIn().getBody(String.class);
                    exchange.getIn().setBody(body);
                })
                .removeHeaders("*")
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .log("CheckPolicyStatus route's body is:${body}, Headers:${in.headers}")
                .endChoice()
                .endDoTry()
                .doCatch(SQLException.class)
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
                .setHeader("Content-Type", constant("application/json"))
                .setBody(simple("{\"error\": \"Data not exist\", \"message\": \"${exception.message}\"}"))
                .log("Database error occurred: ${exception.message}")
                .doCatch(Exception.class)
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
                .setHeader("Content-Type", constant("application/json"))
                .setBody(simple("Unknown error occurred: ${exception.message}"))
                .log("Unknown error occurred: ${exception.message}")
                .end();
    }
}
