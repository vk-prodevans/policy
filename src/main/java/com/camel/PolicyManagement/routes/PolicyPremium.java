package com.camel.PolicyManagement.routes;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.component.sql.SqlComponent;
import org.springframework.stereotype.Component;

@Component
public class PolicyPremium extends RouteBuilder {
    @Override
    public void configure() throws Exception {

        getContext().getComponent("sql", SqlComponent.class);

        restConfiguration().component("jetty")
                .port(8084).host("0.0.0.0")
                .bindingMode(RestBindingMode.json);

        // Global CORS Handling
        interceptFrom().process(exchange -> {
            exchange.getIn().setHeader("Access-Control-Allow-Origin", "*");
            exchange.getIn().setHeader("Access-Control-Allow-Methods", "POST, PUT, OPTIONS, DELETE, GET");
            exchange.getIn().setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
        });

        from("rest:options:addPolicy")
                .routeId("options-handler")
                .setHeader("Access-Control-Allow-Origin", constant("*"))
                .setHeader("Access-Control-Allow-Methods", constant("POST, PUT, GET, OPTIONS, DELETE"))
                .setHeader("Access-Control-Allow-Headers", constant("Origin, X-Requested-With, Content-Type, Accept"))
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(204))
                .setBody(constant(""));

        from("rest:post:addPolicy")
                .routeId("add-policy-premium")
                .doTry()
                    .unmarshal().json()
                    .process(exchange -> {
                        try {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> bodyMap = exchange.getIn().getBody(Map.class);

                            Integer policytype_id = (Integer) bodyMap.get("policytype_id");
                            String policytype_name = (String) bodyMap.get("policytype_name");
                            String policytype_type = (String) bodyMap.get("policytype_type");
                            BigDecimal policytype_interest_rate = new BigDecimal((String) bodyMap.get("policytype_interest_rate"));
                            BigDecimal policytype_max_amount_issuable = new BigDecimal((String) bodyMap.get("policytype_max_amount_issuable"));
                            BigDecimal policytype_min_monthly_income = new BigDecimal((String) bodyMap.get("policytype_min_monthly_income"));

                            // Construct the insert query with all fields including policytype_id
                            String insertQuery = "INSERT INTO policy_type_table (policytype_id, policytype_name, policytype_type, policytype_interest_rate, policytype_max_amount_issuable, policytype_min_monthly_income) "
                                                + "VALUES (" + policytype_id + ", '"
                                                + policytype_name + "', '"
                                                + policytype_type + "', "
                                                + policytype_interest_rate + ", "
                                                + policytype_max_amount_issuable + ", "
                                                + policytype_min_monthly_income + ")";

                            exchange.getIn().setBody(insertQuery);
                        } catch (Exception e) {
                            // Handle exceptions
                            exchange.setException(e);
                        }
                    })
                    .to("jdbc:dataSource")
                    .log("Insert policy_plan_route body is: ${body}, Headers: ${in.headers}")
                    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                //     .setHeader("Access-Control-Allow-Origin", constant("*"))
                    .setBody(exchange -> "{\"success\": true, \"message\": \"Created a new Policy Successfully\"}")
                .doCatch(SQLException.class)
                    .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
                    .setBody(simple("{\"error\": \"Database error occurred: ${exception.message}\"}"))
                    .log("POST Route: {\"error\": \"SQLException occurred: ${exception.message}\"}")
                .doCatch(IllegalArgumentException.class)
                    .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
                    .setBody(simple("{\"error\": \"${exception.message}\"}"))
                    .log("POST Route: {\"error\": \"IllegalArgumentException occurred: ${exception.message}\"}")
                .doCatch(Exception.class)
                    .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
                    .setBody(simple("{\"error\": \"${exception.message}\"}"))
                    .log("POST Route: {\"error\": \"Error processing request: ${exception.message}\"}")
                    .removeHeaders("*", "Content-Type")
                .setHeader("Access-Control-Allow-Origin", constant("*"))
                .setHeader("Access-Control-Allow-Headers",constant("Content-Type"))
                .end();
    }
}
