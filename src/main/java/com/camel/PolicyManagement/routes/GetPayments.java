package com.camel.PolicyManagement.routes;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

public class GetPayments extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        restConfiguration().component("jetty")
                .port(8084).host("0.0.0.0")
                .bindingMode(RestBindingMode.json);
        /*
         * 
         */
        from("rest:get:getPayments/{userId}")
                .routeId("getpayments")
                .log("1:getPaymentsroute started at: ${date:now:yyyy-MM-dd HH:mm:ss}")
                .setHeader("Access-Control-Allow-Credentials", constant("true"))
                .doTry()
                .process(exchange -> {
                    String userId = exchange.getIn().getHeader("userId", String.class);
                    exchange.getIn().setHeader("userId", userId);
                })
                .setBody(simple(
                        "SELECT p.payment_id, p.payment_amount, p.payment_description " +
                                "FROM payments_table p " +
                                "INNER JOIN customer_table c ON p.user_id = c.user_id " +
                                "WHERE c.user_id = ${header.userId}"))

                .to("jdbc:datasource")
                .marshal().json()
                .removeHeaders("*")
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .setHeader("Access-Control-Allow-Origin", constant("*"))
                .log("2:getPayments route's body is: ${body}, Headers: ${in.headers}")
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
