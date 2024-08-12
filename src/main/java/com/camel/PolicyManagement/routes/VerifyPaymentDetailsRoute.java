package com.camel.PolicyManagement.routes;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.sql.SqlComponent;
import org.apache.camel.model.rest.RestBindingMode;

import java.sql.SQLException;

public class VerifyPaymentDetailsRoute extends RouteBuilder {
    @Override
    public void configure() throws Exception {

        getContext().getComponent("sql", SqlComponent.class);

        restConfiguration().component("jetty")
                .port(8084).host("0.0.0.0")
                .bindingMode(RestBindingMode.json);

        from("rest:get:verifyPaymentDetails/{insuranceId}")
                .routeId("verifyPaymentDetails")
                .log("VerifyPaymentDetails route started at: ${date:now:yyyy-MM-dd HH:mm:ss}")
                .doTry()
                .process(exchange -> {
                    String insuranceId = exchange.getIn().getHeader("insuranceId", String.class);
                    exchange.getIn().setHeader("insuranceId", insuranceId);
                })
                .setBody(simple(
                        "SELECT a.*, i.*, p.* " +
                                "FROM application_table a " +
                                "LEFT JOIN insurance_table i ON a.insurance_id = i.insurance_id " +
                                "LEFT JOIN payments_table p ON i.insurance_id = p.insurance_id " +
                                "WHERE i.insurance_id = ${header.insuranceId}"))
                .to("jdbc:datasource")
                .marshal().json()
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .log("VerifyPaymentDetails route's body is: ${body}, Headers: ${in.headers}")
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
