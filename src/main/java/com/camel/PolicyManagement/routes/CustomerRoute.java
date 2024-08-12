package com.camel.PolicyManagement.routes;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.Exchange;
import org.springframework.boot.json.JsonParseException;
import org.springframework.stereotype.Component;
import java.sql.SQLException;
import java.util.Map;

@Component
public class CustomerRoute extends RouteBuilder {

    @Override
    public void configure() {

        // Configure REST endpoint
        restConfiguration().component("jetty")
                .port(8084)
                .host("0.0.0.0")
                .bindingMode(RestBindingMode.json);

        // Add a global CORS filter
        interceptFrom().process(exchange -> {
            exchange.getIn().setHeader("Access-Control-Allow-Origin", "*");
            exchange.getIn().setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PUT");
            exchange.getIn().setHeader("Access-Control-Allow-Headers",
                    "Origin, X-Requested-With, Content-Type, Accept");
        });

        from("rest:options:addCustomer")
                .routeId("options-customer")
                .setHeader("Access-Control-Allow-Origin", constant("*"))
                .setHeader("Access-Control-Allow-Methods", constant("POST, GET, OPTIONS, DELETE, PUT"))
                .setHeader("Access-Control-Max-Age", constant("3600"))
                .setHeader("Access-Control-Allow-Headers",
                        constant("Origin, X-Requested-With, Content-Type, Accept"))
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200));

        from("rest:post:addCustomer")
                .routeId("post-customer")
                .doTry()
                .unmarshal().json()
                .process(exchange -> {
                    try {
                        // Extract data from request body
                        @SuppressWarnings("unchecked")
                        Map<String, Object> bodyMap = exchange.getIn().getBody(Map.class);
                        String user_id = (String) bodyMap.get("user_id");
                        String customer_firstname = (String) bodyMap.get("customer_firstname");
                        String customer_lastname = (String) bodyMap.get("customer_lastname");
                        String customer_email = (String) bodyMap.get("customer_email");
                        String customer_dob = (String) bodyMap.get("customer_dob");
                        String customer_username = (String) bodyMap.get("customer_username");
                        String customer_address = (String) bodyMap.get("customer_address");
                        String customer_gender = (String) bodyMap.get("customer_gender");
                        String customer_role = (String) bodyMap.get("customer_role");

                        // Construct SQL query for insertion
                        String insertQuery = "INSERT INTO customer_table (user_id, customer_firstname, customer_lastname, customer_email, "
                                + "customer_dob, customer_username, customer_address, customer_gender, customer_role) "
                                + "VALUES ('" + user_id + "', '"
                                + customer_firstname + "', '"
                                + customer_lastname + "', '"
                                + customer_email + "', '" + customer_dob + "', '"
                                + customer_username + "', '" + customer_address + "', '"
                                + customer_gender + "', '" + customer_role + "')";

                        exchange.getIn().setBody(insertQuery);

                    } catch (Exception e) {
                        // Handle exceptions
                        exchange.setException(e);
                    }
                })
                .to("jdbc:dataSource")
                .log("post-customer Customer created successfully")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
                .setHeader("Content-Type", constant("application/json"))
                .setBody(simple("{\"success\": true, \"message\": \"Customer created successfully.\"}"))
                .doCatch(SQLException.class)
                .log("post-customer SQL Exception: ${exception.message}")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
                .setBody(simple("{\"success\": false, \"message\": \"SQL Exception: ${exception.message}\"}"))
                .doCatch(JsonParseException.class)
                .log("post-customer JSON Parse Exception: ${exception.message}")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
                .setBody(simple("{\"success\": false, \"message\": \"JSON Parse Exception: ${exception.message}\"}"))
                .doCatch(Exception.class)
                .log("post-customer Error processing request: ${exception.message}")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
                .setBody(
                        simple("{\"success\": false, \"message\": \"Error processing request: ${exception.message}\"}"))
                .end()
                .removeHeaders("*", "Content-Type")
                .setHeader("Access-Control-Allow-Origin", constant("*"))
                .setHeader("Access-Control-Allow-Headers", constant("Content-Type"));

        from("rest:get:fetchCustomerName")
                .setHeader("Access-Control-Allow-Credentials", constant("true"))
                .log("fetching the userId route started at: ${date:now:yyyy-MM-dd HH:mm:ss}")
                .doTry()
                .setBody(constant("SELECT user_id FROM customer_table"))
                .to("jdbc:datasource")
                .marshal().json()
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .log("Fetched policy plan names: ${body}, Headers: ${in.headers}")
                .endDoTry()
                .doCatch(SQLException.class)
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
                .setHeader("Content-Type", constant("application/json"))
                .setBody(simple("{\"error\": \"Database error\", \"message\": \"Unable to retrieve policy plans\"}"))
                .log("Database error occurred: ${exception.message}")
                .doCatch(Exception.class)
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
                .setHeader("Content-Type", constant("application/json"))
                .setBody(simple("{\"error\": \"Unknown error\", \"message\": \"An unexpected error occurred\"}"))
                .log("Unknown error occurred: ${exception.message}")
                .end();

                from("rest:get:fetchAllPolicyPlans")
                .setHeader("Access-Control-Allow-Credentials", constant("true"))
                .log("fetching the userId route started at: ${date:now:yyyy-MM-dd HH:mm:ss}")
                .doTry()
                .setBody(constant("SELECT policytype_name FROM policy_type_table"))
                .to("jdbc:datasource")
                .marshal().json()
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .log("Fetched policy plan names: ${body}, Headers: ${in.headers}")
                .endDoTry()
                .doCatch(SQLException.class)
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
                .setHeader("Content-Type", constant("application/json"))
                .setBody(simple("{\"error\": \"Database error\", \"message\": \"Unable to retrieve policy plans\"}"))
                .log("Database error occurred: ${exception.message}")
                .doCatch(Exception.class)
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
                .setHeader("Content-Type", constant("application/json"))
                .setBody(simple("{\"error\": \"Unknown error\", \"message\": \"An unexpected error occurred\"}"))
                .log("Unknown error occurred: ${exception.message}")
                .end();
    }
}
