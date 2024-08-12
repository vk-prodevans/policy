package com.camel.PolicyManagement.routes;

import java.math.BigDecimal;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.sql.SqlComponent;
import org.apache.camel.model.rest.RestBindingMode;

public class MakePaymentRoute extends RouteBuilder {
    @SuppressWarnings("deprecation")
    @Override
    public void configure() throws Exception {

        getContext().getComponent("sql", SqlComponent.class);

        restConfiguration().component("jetty")
                .port(8084).host("0.0.0.0")
                .bindingMode(RestBindingMode.json).enableCORS(true)
                .corsHeaderProperty("Access-Control-Allow-Origin", "*")
                .corsHeaderProperty("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
                .corsHeaderProperty("Access-Control-Allow-Headers",
                        "Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers,insuranceId");

        from("rest:post:addPaymentStatus/")
                .routeId("post-payment-status")
                .setHeader("Access-Control-Allow-Credentials", constant("true"))
                .doTry()
                .unmarshal().json()
                .process(exchange -> {
                    try {
                        // Extract data from request body
                        @SuppressWarnings("unchecked")
                        Map<String, Object> bodyMap = exchange.getIn().getBody(Map.class);
                        Integer payment_status_id = (Integer) bodyMap.get("payment_status_id");
                        BigDecimal payment_status_amount = new BigDecimal(
                                (String) bodyMap.get("payment_status_amount"));
                        String payment_status = (String) bodyMap.get("payment_status");
                        String payment_time_date = (String) bodyMap.get("payment_time_date");
                        Integer payment_id = (Integer) bodyMap.get("payment_id");
                        Integer customer_id = (Integer) bodyMap.get("customer_id");
                        Integer insurance_id = (Integer) bodyMap.get("insurance_id");

                        // Construct SQL query for insertion
                        String insertQuery = "INSERT INTO payment_status_table (payment_status_id, payment_status_amount, payment_status, payment_time_date";
                        String valuesClause = " VALUES (" + payment_status_id + ", " + payment_status_amount + ", '"
                                + payment_status + "', '" + payment_time_date + "'";

                        // Append nullable fields if they are not null
                        if (payment_id != null) {
                            insertQuery += ", payment_id";
                            valuesClause += ", " + payment_id;
                        }
                        if (customer_id != null) {
                            insertQuery += ", customer_id";
                            valuesClause += ", " + customer_id;
                        }
                        if (insurance_id != null) {
                            insertQuery += ", insurance_id";
                            valuesClause += ", " + insurance_id;
                        }

                        insertQuery += ")";
                        valuesClause += ")";

                        exchange.getIn().setBody(insertQuery + valuesClause);
                    } catch (Exception e) {
                        // Handle exceptions
                        throw new RuntimeException("Error processing request", e);
                    }
                })
                .to("jdbc:dataSource")
                .doCatch(Exception.class)
                .process(exchange -> {
                    Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    // Log or handle the exception
                    log.error("Error processing request: {}", cause.getMessage());
                    exchange.getOut().setBody("An error occurred while processing your request.");
                    exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, 500); // Internal Server Error
                })
                .end();
    }
}