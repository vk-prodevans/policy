package com.camel.PolicyManagement.routes;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.sql.SqlComponent;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.boot.json.JsonParseException;

public class InsuranceRoute extends RouteBuilder {

        @Override
        public void configure() throws Exception {
                getContext().getComponent("sql", SqlComponent.class);

                restConfiguration().component("jetty")
                                .port(8084).host("0.0.0.0")
                                .bindingMode(RestBindingMode.json);

                // Route for CORS preflight requests
                from("rest:options:addInsurance")
                                .routeId("options-customer")
                                .setHeader("Access-Control-Allow-Origin", constant("*"))
                                .setHeader("Access-Control-Allow-Methods", constant("POST, GET, OPTIONS, DELETE, PUT"))
                                .setHeader("Access-Control-Max-Age", constant("3600"))
                                .setHeader("Access-Control-Allow-Headers",
                                                constant("Origin, X-Requested-With, Content-Type, Accept,user_id"))
                                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(204));

                /*
                 * Route for adding insurance
                 */
                from("rest:post:addInsurance").routeId("post-insurance")
                                .doTry()
                                .unmarshal().json()
                                .process(exchange -> {
                                        try {
                                                // Extract data from request body
                                                @SuppressWarnings("unchecked")
                                                Map<String, Object> bodyMap = exchange.getIn().getBody(Map.class);

                                                if (bodyMap == null || bodyMap.isEmpty()) {
                                                        throw new IllegalArgumentException("Request body is empty");
                                                }

                                                String userId = exchange.getIn().getHeader("user_id", String.class);
                                                if (userId == null) {
                                                        throw new IllegalArgumentException("user_id header is missing");
                                                }

                                                String insurance_plan = (String) bodyMap.get("insurance_plan");
                                                String insurance_number = (String) bodyMap.get("insurance_number");
                                                String insurance_issue_date = (String) bodyMap
                                                                .get("insurance_issue_date");
                                                String monthly_income = (String) bodyMap.get("monthly_income");
                                                String insurance_expire_date = (String) bodyMap
                                                                .get("insurance_expire_date");
                                                BigDecimal insurance_premium = new BigDecimal(
                                                                (String) bodyMap.get("insurance_premium"));
                                                BigDecimal insurance_amount = new BigDecimal(
                                                                (String) bodyMap.get("insurance_amount"));

                                                // Construct SQL query for insertion with user_id
                                                String insertQuery = "INSERT INTO insurance_table (insurance_plan, insurance_number, insurance_issue_date, "
                                                                + "monthly_income, insurance_expire_date, insurance_premium, insurance_amount, user_id) "
                                                                + "VALUES ('" + insurance_plan + "', '"
                                                                + insurance_number + "', '"
                                                                + insurance_issue_date + "', '"
                                                                + monthly_income + "', '"
                                                                + insurance_expire_date + "', '"
                                                                + insurance_premium.toString() + "', '"
                                                                + insurance_amount.toString() + "', '"
                                                                + userId + "')RETURNING insurance_id";

                                                exchange.getIn().setBody(insertQuery);

                                                // Create a response map to store the values
                                                Map<String, Object> responseMap = new HashMap<>();
                                                
                                                responseMap.put("insurance_plan", insurance_plan);
                                                responseMap.put("insurance_number", insurance_number);
                                                responseMap.put("insurance_issue_date", insurance_issue_date);
                                                responseMap.put("monthly_income", monthly_income);
                                                responseMap.put("insurance_expire_date", insurance_expire_date);
                                                responseMap.put("insurance_premium", insurance_premium);
                                                responseMap.put("insurance_amount", insurance_amount);
                                                responseMap.put("user_id", userId);

                                                // Set the response map as a header for logging purposes
                                                exchange.getIn().setHeader("responseMap", responseMap);

                                        } catch (Exception e) {
                                                // Handle exceptions
                                                exchange.setException(e);
                                        }
                                })
                                .to("jdbc:dataSource")
                                .log("post-insurance Insurance created successfully")
                                .process(exchange -> {
                                        Integer insuranceId = (Integer) ((Map<String, Object>) exchange.getIn().getBody(List.class).get(0)).get("insurance_id");
                                        // Retrieve the response map from the header
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> responseMap = exchange.getIn().getHeader("responseMap",
                                                        Map.class);
                                        responseMap.put("insurance_id", insuranceId);
                                        // Convert the response map to JSON format
                                        exchange.getIn().setBody(responseMap);
                                })
                                .marshal().json()
                                .log("body is ${body}")
                                // .setHeader("insurance_id", simple("${exchangeProperty.generatedKeys[0][0]}"))
                                // // assuming your database supports generated keys
                                .to("direct:addApplication")
                                .removeHeaders("*", "Content-Type")
                                .setHeader("Access-Control-Allow-Origin", constant("*"))
                                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
                                .setHeader("Content-Type", constant("application/json"))
                                .setBody(simple("{\"success\": true, \"message\": \"Insurance created successfully.\"}"))
                                .doCatch(SQLException.class)
                                .log("post-insurance SQL Exception: ${exception.message}")
                                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
                                .setBody(simple("{\"success\": false, \"message\": \"SQL Exception: ${exception.message}\"}"))
                                .doCatch(JsonParseException.class)
                                .log("post-insurance JSON Parse Exception: ${exception.message}")
                                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
                                .setBody(simple("{\"success\": false, \"message\": \"JSON Parse Exception: ${exception.message}\"}"))
                                .doCatch(Exception.class)
                                .log("post-insurance Error processing request: ${exception.message}")
                                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
                                .setBody(
                                                simple("{\"success\": false, \"message\": \"Error processing request: ${exception.message}\"}"))
                                .end().removeHeaders("*", "Content-Type")
                                .setHeader("Access-Control-Allow-Origin", constant("*"))
                                .setHeader("Access-Control-Allow-Headers", constant("Content-Type"));

                /*
                 * Route for adding application details
                 */
                /*
                 * Route for adding application details
                 */
                from("direct:addApplication").routeId("add-application")
                .log("body is ${body}")
                .unmarshal().json(JsonLibrary.Jackson, Map.class)
                .process(exchange -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> bodyMap = exchange.getIn().getBody(Map.class);
    
                    BigDecimal monthlyIncome = convertToBigDecimal(bodyMap.get("monthly_income"));
                    BigDecimal insuranceAmount = convertToBigDecimal(bodyMap.get("insurance_amount"));
                    String insuranceNumber = convertToString(bodyMap.get("insurance_number"));
                    Integer insuranceId = (Integer) bodyMap.get("insurance_id");
                    String applicationStatus = "pending";
                    Date applicationDate = new Date();
                    BigDecimal applicationCoverageAmount = insuranceAmount.multiply(new BigDecimal("0.60"));
    
                    // Construct SQL query for application insertion
                    String insertQuery = "INSERT INTO application_table (application_status, application_date, "
                        + "application_monthly_income, application_coverage_amount, insurance_number, insurance_id) "
                        + "VALUES ('" + applicationStatus + "', '"
                        + new java.sql.Date(applicationDate.getTime()) + "', '"
                        + monthlyIncome.toString() + "', '"
                        + applicationCoverageAmount.toString() + "', '"
                        + insuranceNumber + "', "
                        + insuranceId + ")";
    
                    exchange.getIn().setBody(insertQuery);
                })
                .to("jdbc:dataSource")
                .log("Application created successfully");
        }
    
        private BigDecimal convertToBigDecimal(Object value) {
            if (value instanceof String) {
                return new BigDecimal((String) value);
            } else if (value instanceof Number) {
                return BigDecimal.valueOf(((Number) value).doubleValue());
            } else {
                throw new IllegalArgumentException("Cannot convert value to BigDecimal: " + value);
            }
        }
    
        private String convertToString(Object value) {
            if (value instanceof String) {
                return (String) value;
            } else if (value != null) {
                return value.toString();
            } else {
                throw new IllegalArgumentException("Cannot convert value to String: " + value);
            }
        }
    }