package com.camel.PolicyManagement.routes;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.component.sql.SqlComponent;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.camel.PolicyManagement.model.Application;
import com.camel.PolicyManagement.util.PdfConverter;

@Component
public class PaymentCalculationRoute extends RouteBuilder {
    @Autowired
    PdfConverter pdfConverter;

    @Override
    public void configure() throws Exception {
        // Configure properties component to load properties from external file
        PropertiesComponent properties = (PropertiesComponent) getContext().getPropertiesComponent();
        properties.setLocation("classpath:application.properties");
        getContext().getComponent("sql", SqlComponent.class);

        // REST configuration
        restConfiguration().component("jetty")
                .port(8084).host("0.0.0.0")
                .bindingMode(RestBindingMode.json);

        // Route for handling payment calculation and insertion
        from("rest:post:calculatePayment")
                .routeId("calculatePayment")
                .log("calculatePayment route started at: ${date:now:yyyy-MM-dd HH:mm:ss}")
                .doTry()
                .unmarshal().json(JsonLibrary.Jackson, Map.class)
                .process(exchange -> {
                    // Extract insurance ID from the request body and set it in headers
                    @SuppressWarnings("unchecked")
                    Map<String, Object> body = exchange.getIn().getBody(Map.class);
                    Integer insuranceId = (Integer) body.get("insuranceId");
                    if (insuranceId == null) {
                        throw new IllegalArgumentException("Insurance ID is missing in the request body");
                    }
                    exchange.getIn().setHeader("insuranceId", insuranceId);
                })
                .setBody(simple(
                    "SELECT i.insurance_id, i.user_id, i.insurance_premium, i.insurance_amount, i.insurance_plan, a.* " +
                    "FROM insurance_table i " +
                    "LEFT JOIN application_table a ON i.insurance_id = a.insurance_id " +
                    "WHERE i.insurance_id = ${header.insuranceId}"))
                .to("jdbc:datasource")
                .process(exchange -> {
                    @SuppressWarnings("unused")
                    String currentMonth = java.time.LocalDate.now().getMonth().toString();
                    int currentday = java.time.LocalDate.now().getDayOfMonth();
                    int payment_date = currentday + 5;
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> resultList = exchange.getIn().getBody(List.class);
                    if (resultList != null && !resultList.isEmpty()) {
                        Map<String, Object> resultMap = resultList.get(0);
                        BigDecimal minMonthlyIncome = (BigDecimal) resultMap.get("application_monthly_income");
                        BigDecimal maxAmountIssuable = (BigDecimal) resultMap.get("application_coverage_amount");
                        BigDecimal payment_amount = maxAmountIssuable.multiply(BigDecimal.valueOf(0.66));
                        BigDecimal minMontly_income = minMonthlyIncome.multiply(BigDecimal.valueOf(0.10));
                        String payment_description = (String) resultMap.get("insurance_plan");
                        Integer insurance_id = (Integer) resultMap.get("insurance_id");
                        String user_id = (String) resultMap.get("user_id");

                        Map<String, Object> responseBody = new LinkedHashMap<>();
                        responseBody.put("minMontly_income", minMontly_income);
                        responseBody.put("payment_amount", payment_amount);
                        responseBody.put("payment_description", payment_description);
                        responseBody.put("payment_date", payment_date);
                        responseBody.put("insurance_id", insurance_id);
                        responseBody.put("user_id", user_id);
                        exchange.getMessage().setBody(responseBody);
                    } else {
                        throw new RuntimeException("No policy found for the provided insurance ID");
                    }
                })
                .marshal().json()
                .removeHeaders("*")
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .to("direct:addpayments")
                .log("calculatePayment route's body is: ${body}, Headers: ${in.headers}")
                .doCatch(Exception.class)
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
                .setHeader("Content-Type", constant("application/json"))
                .setBody(simple("Error processing request: ${exception.message}"))
                .log("Error processing request: ${exception.message}")
                .end();

        // Route to handle payment insertion
        from("direct:addpayments")
                .log("Processing payment insertion")
                .unmarshal().json(JsonLibrary.Jackson, Map.class)
                .process(exchange -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> body = exchange.getIn().getBody(Map.class);
                    if (body == null) {
                        throw new IllegalArgumentException("Request body is null");
                    }

                    BigDecimal minMontly_income = new BigDecimal(body.getOrDefault("minMontly_income", BigDecimal.ZERO).toString());
                    BigDecimal payment_amount = new BigDecimal(body.getOrDefault("payment_amount", BigDecimal.ZERO).toString());
                    String payment_description = (String) body.getOrDefault("payment_description", "");
                    Integer payment_date = body.get("payment_date") != null ? ((Number) body.get("payment_date")).intValue() : null;
                    Integer insurance_id = body.get("insurance_id") != null ? ((Number) body.get("insurance_id")).intValue() : null;
                    String user_id = (String) body.get("user_id");

                    if (payment_date == null ) {
                        throw new IllegalArgumentException("Missing required field(s): ");
                    }

                    String insertQuery = String.format(
                            "INSERT INTO payments_table (minMontly_income, payment_date, payment_amount, payment_description, insurance_id, user_id) " +
                            "VALUES (%s, %d, %s, '%s', %d, '%s')",
                            minMontly_income, payment_date, payment_amount, payment_description, insurance_id, user_id);
                    exchange.getIn().setBody(insertQuery);
                })
                .to("jdbc:datasource")
                .log("Payment inserted successfully: ${body}");
    }
}
