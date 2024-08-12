package com.camel.PolicyManagement.routes;

import java.sql.SQLException;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.sql.SqlComponent;
import org.apache.camel.model.TryDefinition;
import org.apache.camel.model.rest.RestBindingMode;

public class GetPoliciesRoute extends RouteBuilder {
     @Override
    public void configure() throws Exception {
        getContext().getComponent("sql", SqlComponent.class);

        restConfiguration().component("jetty")
                .port(8084).host("0.0.0.0")
                .bindingMode(RestBindingMode.json);

        // // Handle CORS preflight requests
        from("rest:options:getPolicies")
        .setHeader("Access-Control-Allow-Origin", constant("*"))
        .setHeader("Access-Control-Allow-Methods", constant("POST, GET, OPTIONS, DELETE, PUT"))
        .setHeader("Access-Control-Max-Age", constant("3600"))
        .setHeader("Access-Control-Allow-Headers", constant("Origin, X-Requested-With, Content-Type, Accept"))
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(204))
        .end();

// Define REST POST endpoint for getting all policies
((TryDefinition) from("rest:post:getPolicies")
        .routeId("getPolicies")
        .log("getPolicies route started at: ${date:now:yyyy-MM-dd HH:mm:ss}")
        .doTry()
        .unmarshal().json()
        .process(exchange -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> requestBody = exchange.getIn().getBody(Map.class);
            String customerRole = requestBody.get("customerRole").toString();
            exchange.getIn().setHeader("customerRole", customerRole);
        })
        .log("getpolicies route Headers before choice: ${headers}")
        .choice()
        .when(header("customerRole").isEqualTo("Approver"))
        .setBody(simple("SELECT i.insurance_id, i.insurance_plan, i.insurance_number, " +
                "i.insurance_issue_date, i.insurance_premium, i.insurance_amount, " +
                "i.insurance_description, i.insurance_expire_date, " +
                "a.application_status_id, a.application_status, a.application_date, " +
                "a.application_monthly_income, a.application_coverage_amount, " +
                "a.application_start_time_duration, a.application_end_time_duration, " +
                "c.user_id, c.customer_firstname, c.customer_lastname, " +
                "c.customer_email, c.customer_dob, c.customer_username, " +
                "c.customer_address, c.customer_gender, c.customer_role " +
                "FROM insurance_table i " +
                "JOIN customer_table c ON i.user_id = c.user_id " +
                "LEFT JOIN application_table a ON i.insurance_id = a.insurance_id"))
        .when(header("customerRole").isEqualTo("policyMaker"))
        .setBody(simple("SELECT i.insurance_number, i.insurance_amount,i.insurance_id, " +
                "p.minMontly_income, p.payment_date, p.payment_amount, p.payment_description, " +
                "a.application_monthly_income, a.application_date, a.application_coverage_amount, a.application_status," +
                "c.user_id, c.customer_firstname, c.customer_lastname, " +
                "c.customer_email, c.customer_dob, c.customer_username, " +
                "c.customer_address, c.customer_gender, c.customer_role " +
                "FROM insurance_table i " +
                "JOIN customer_table c ON i.user_id = c.user_id " +
                "LEFT JOIN payments_table p ON i.insurance_id = p.insurance_id " +
                "LEFT JOIN application_table a ON i.insurance_id = a.insurance_id"))
        .otherwise()
        .setBody(simple("Invalid customer role"))
        .end()
        .log("getpolicies route Executing SQL query: ${body}")
        .to("jdbc:datasource")
        .log("getpolicies route SQL query executed, raw result: ${body}")
        .marshal().json()
        .process(exchange -> {
            String body = exchange.getIn().getBody(String.class);
            exchange.getIn().setBody(body);
        })
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .setHeader("Access-Control-Allow-Origin", constant("*"))
        .log("getPolicies route's body is: ${body}, Headers: ${in.headers}"))
        .doCatch(SQLException.class)
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
        .setHeader("Content-Type", constant("application/json"))
        .setBody(simple("Error while fetching policies: Database error occurred"))
        .log("getPolicies route's SQLException occurred: ${exception.message}")
        .doCatch(Exception.class)
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
        .setHeader("Content-Type", constant("application/json"))
        .setBody(simple("Error processing request: ${exception.message}"))
        .log("getPolicies route Error processing request: ${exception.message}")
        .end();
    }
}
