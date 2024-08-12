package com.camel.PolicyManagement.routes;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.sql.SqlComponent;
import org.apache.camel.model.rest.RestBindingMode;

public class FetchPaymentAmountRoute extends RouteBuilder {
    @Override
    public void configure() throws Exception {

        getContext().getComponent("sql", SqlComponent.class);

        restConfiguration().component("jetty")
                .port(8084).host("0.0.0.0")
                .bindingMode(RestBindingMode.json);

        from("rest:get:fetchPaymentAmount/{insuranceId}")
                .routeId("fetchPaymentAmount")
                .log("FetchPaymentAmount route started at: ${date:now:yyyy-MM-dd HH:mm:ss}")
                .doTry()
                .process(exchange -> {
                    String insuranceId = exchange.getIn().getHeader("insuranceId", String.class);
                    exchange.getIn().setHeader("insuranceId", insuranceId);
                })
                .setBody(simple(
                        "SELECT i.insurance_id, p.payment_amount " +
                                "FROM insurance_table i " +
                                "LEFT JOIN payments_table p ON i.insurance_id = p.insurance_id " +
                                "WHERE i.insurance_id = ${header.insuranceId}"))
                .to("jdbc:datasource")
                .choice()
                .when(simple("${body} == null || ${body.isEmpty()}"))
                .throwException(new Exception("Payment not found for the insurance ID"))
                .otherwise()
                .marshal().json()
                .endChoice()
                .endDoTry()
                .doCatch(Exception.class)
                .setHeader("Content-Type", constant("application/json"))
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
                .setBody(simple(
                        "{\"error\": \"Failed to fetch payment amount\", \"message\": \"${exception.message}\"}"))
                .log("Error occurred while fetching payment amount: ${exception.message}")
                .end();
    }
}
