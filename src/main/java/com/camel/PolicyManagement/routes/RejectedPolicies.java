package com.camel.PolicyManagement.routes;

import java.sql.SQLException;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.sql.SqlComponent;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

public class RejectedPolicies extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        getContext().getComponent("sql", SqlComponent.class);

        restConfiguration().component("jetty")
                .port(8084).host("0.0.0.0")
                .bindingMode(RestBindingMode.json);

        // CORS headers for the route
        from("rest:get:getRejectedPolicies")
                .routeId("getPolicies")
                .setHeader("Access-Control-Allow-Credentials", constant("true"))
                .log("getPolicies route started at: ${date:now:yyyy-MM-dd HH:mm:ss}")
                .doTry()
                .setBody(simple("SELECT i.insurance_id, i.insurance_description, a.application_status, c.user_id, c.customer_username " +
                        "FROM insurance_table i " +
                        "JOIN customer_table c ON i.user_id = c.user_id " +
                        "LEFT JOIN application_table a ON i.insurance_id = a.insurance_id " +
                        "WHERE a.application_status = 'Rejected'"))
                .to("jdbc:datasource")
                .marshal().json()
                .removeHeaders("*")
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .setHeader("Access-Control-Allow-Origin", constant("*"))
                .log("getPolicies route's body is:${body}, Headers:${in.headers}")
                .doCatch(SQLException.class)
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
                .setHeader("Content-Type", constant("application/json"))
                .setBody(simple("Error while fetching policies: Database error occurred"))
                .log("getPolicies route's SQLException occurred: ${exception.message}")
                .doCatch(Exception.class)
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
                .setHeader("Content-Type", constant("application/json"))
                .setBody(simple("Error processing request: ${exception.message}"))
                .log("Error processing request: ${exception.message}")
                .end();
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("*"); // Allow your frontend URL
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/", config);
        return new CorsFilter(source);
    }
}
