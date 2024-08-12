package com.camel.PolicyManagement.routes;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.stereotype.Component;

@Component
public class UpdatePolicy extends RouteBuilder {
        @Override
        public void configure() throws Exception {

                // Configure REST endpoint
                restConfiguration().component("jetty")
                                .port(8084)
                                .host("0.0.0.0")
                                .bindingMode(RestBindingMode.json)
                                .enableCORS(true);

                // Global CORS headers
                interceptFrom().process(exchange -> {
                        exchange.getIn().setHeader("Access-Control-Allow-Origin", "*");
                        exchange.getIn().setHeader("Access-Control-Allow-Methods", "PUT, GET, OPTIONS, DELETE, POST");
                        exchange.getIn().setHeader("Access-Control-Allow-Headers",
                                        "Origin, X-Requested-With, Content-Type, Accept");
                        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
                });

                // CORS for OPTIONS requests
                from("rest:options:updatePolicy/{policytypeId}")
                                .routeId("options-addPaymentStatus")
                                .setHeader("Access-Control-Allow-Origin", constant("*"))
                                .setHeader("Access-Control-Allow-Methods", constant("POST, GET, OPTIONS, DELETE, PUT"))
                                .setHeader("Access-Control-Max-Age", constant("3600"))
                                .setHeader("Access-Control-Allow-Headers",
                                                constant("Origin, X-Requested-With, Content-Type, Accept"))
                                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
                                .log("OPTIONS Route: Headers set for CORS");

                // PUT request handler
                from("rest:put:updatePolicy/{policytypeId}")
                                .routeId("update-policy-plan")
                                .log("PUT Route: ${routeId} started at: ${date:now:yyyy-MM-dd HH:mm:ss}")
                                .doTry()
                                .unmarshal().json()
                                .log("Request Body: ${body}")
                                .process(exchange -> {
                                        try {
                                                // Extract data from request body
                                                @SuppressWarnings("unchecked")
                                                Map<String, Object> bodyMap = exchange.getIn().getBody(Map.class);
                                                log.info("Extracted bodyMap: " + bodyMap);

                                                // Extract policytype_id from URL
                                                String policytypeId = exchange.getIn().getHeader("policytypeId",
                                                                String.class);
                                                log.info("Extracted policytypeId: " + policytypeId);

                                                // Extract other data from the body
                                                String policytypeName = (String) bodyMap.get("policytype_name");
                                                String policytypeType = (String) bodyMap.get("policytype_type");
                                                BigDecimal policytypeInterestRate = new BigDecimal(
                                                                bodyMap.get("policytype_interest_rate").toString());
                                                BigDecimal policytypeMaxAmountIssuable = new BigDecimal(bodyMap
                                                                .get("policytype_max_amount_issuable").toString());
                                                BigDecimal policytypeMinMonthlyIncome = new BigDecimal(bodyMap
                                                                .get("policytype_min_monthly_income").toString());

                                                // Log extracted values
                                                log.info("Extracted policytypeName: " + policytypeName);
                                                log.info("Extracted policytypeType: " + policytypeType);
                                                log.info("Extracted policytypeInterestRate: " + policytypeInterestRate);
                                                log.info("Extracted policytypeMaxAmountIssuable: "
                                                                + policytypeMaxAmountIssuable);
                                                log.info("Extracted policytypeMinMonthlyIncome: "
                                                                + policytypeMinMonthlyIncome);

                                                // Construct the update query
                                                String updateQuery = "UPDATE policy_type_table SET " +
                                                                "policytype_name = '" + policytypeName + "', " +
                                                                "policytype_type = '" + policytypeType + "', " +
                                                                "policytype_interest_rate = " + policytypeInterestRate
                                                                + ", " +
                                                                "policytype_max_amount_issuable = "
                                                                + policytypeMaxAmountIssuable + ", " +
                                                                "policytype_min_monthly_income = "
                                                                + policytypeMinMonthlyIncome + " " +
                                                                "WHERE policytype_id = '" + policytypeId + "'";

                                                log.info("Constructed update query: " + updateQuery);
                                                exchange.getIn().setBody(updateQuery);
                                        } catch (Exception e) {
                                                log.error("Exception during processing: " + e.getMessage());
                                                exchange.setException(e);
                                        }
                                })
                                .to("jdbc:dataSource")
                                .log("Update policy_plan_route body is: ${body}, Headers: ${in.headers}")
                                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(201))
                                .setHeader("Access-Control-Allow-Origin", constant("*"))
                                .setHeader("Access-Control-Allow-Headers", constant(
                                                "access-control-allow-methods,access-control-allow-origin,authorization,content-type"))
                                .setHeader("Access-Control-Allow-Methods", constant("GET, DELETE, POST, OPTIONS, PUT"))
                                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                                .setBody(simple("{\"success\": true, \"message\": \"Policy Plan with ID ${header.policytypeId} has been updated\"}"))
                                .log("Response Body: ${body}")
                                .doCatch(SQLException.class)
                                .process(exchange -> {
                                        String errorMessage = "{\"error\": \"Database error occurred: "
                                                        + exchange.getException().getMessage() + "\"}";
                                        exchange.getIn().setBody(errorMessage);
                                        log.error("SQLException occurred: " + errorMessage);
                                })
                                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
                                .doCatch(IllegalArgumentException.class)
                                .process(exchange -> {
                                        String errorMessage = "{\"error\": \"" + exchange.getException().getMessage()
                                                        + "\"}";
                                        exchange.getIn().setBody(errorMessage);
                                        log.error("IllegalArgumentException occurred: " + errorMessage);
                                })
                                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
                                .doCatch(Exception.class)
                                .process(exchange -> {
                                        String errorMessage = "{\"error\": \"" + exchange.getException().getMessage()
                                                        + "\"}";
                                        exchange.getIn().setBody(errorMessage);
                                        log.error("Exception occurred: " + errorMessage);
                                })
                                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
                                .end();
        }
}
