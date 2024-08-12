package com.camel.PolicyManagement.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.postgresql.util.PGobject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GetHeaders extends RouteBuilder {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void configure() throws Exception {
        restConfiguration().component("jetty")
                .port(8084).host("0.0.0.0")
                .bindingMode(RestBindingMode.json);

        interceptFrom().process(exchange -> {
            exchange.getIn().setHeader("Access-Control-Allow-Origin", "*");
            exchange.getIn().setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PUT");
            exchange.getIn().setHeader("Access-Control-Allow-Headers",
                    "Origin, X-Requested-With, Content-Type, Accept");
        });

        from("rest:get:headers")
                .routeId("getHeaders")
                .setHeader("Access-Control-Allow-Credentials", constant("true"))
                .log("getHeaders route started at: ${date:now:yyyy-MM-dd HH:mm:ss}")
                .doTry()
                .setBody(constant(
                        "SELECT table_name, " +
                                "json_build_object(" +
                                "'application_status', application_status, " +
                                "'application_date', application_date, " +
                                "'insurance_expire_date', insurance_expire_date, " +
                                "'insurance_plan', insurance_plan, " +
                                "'insurance_issue_date', insurance_issue_date, " +
                                "'insurance_number', insurance_number, " +
                                "'customer_firstname', customer_firstname, " +
                                "'customer_download', customer_download, " +
                                "'customer_details', customer_details, " +
                                "'selec', \"selec\") as headers " +
                                "FROM headers"))
                .to("jdbc:datasource")
                .process(exchange -> {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> rows = exchange.getIn().getBody(List.class);
                    Map<String, List<Map<String, String>>> result = new LinkedHashMap<>();

                    for (Map<String, Object> row : rows) {
                        String tableName = (String) row.get("table_name");
                        PGobject headersPG = (PGobject) row.get("headers");

                        @SuppressWarnings("unchecked")
                        LinkedHashMap<String, String> headersMap = objectMapper.readValue(headersPG.getValue(),
                                LinkedHashMap.class);

                        LinkedHashMap<String, String> filteredHeadersMap = new LinkedHashMap<>();
                        headersMap.forEach((key, value) -> {
                            if (value != null) {
                                filteredHeadersMap.put(key, value);
                            }
                        });

                        List<Map<String, String>> formattedHeaders = new ArrayList<>();
                        if ("approver_table".equals(tableName) || "policy_issuer_table".equals(tableName)) {
                            String[] orderedKeys = { "application_status", "insurance_number", "customer_firstname",
                                    "application_date", "insurance_plan", "selec" };
                            for (String key : orderedKeys) {
                                if (filteredHeadersMap.containsKey(key)) {
                                    Map<String, String> attrObject = new LinkedHashMap<>();
                                    attrObject.put(key, filteredHeadersMap.get(key));
                                    formattedHeaders.add(attrObject);
                                }
                            }
                        } else {
                            filteredHeadersMap.forEach((key, value) -> {
                                Map<String, String> attrObject = new LinkedHashMap<>();
                                attrObject.put(key, value);
                                formattedHeaders.add(attrObject);
                            });
                        }

                        result.put(tableName, formattedHeaders);
                    }

                    exchange.getIn().setBody(result);
                })

                .marshal().json()
                .removeHeaders("*")
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .setHeader("Access-Control-Allow-Origin", constant("*"))
                .log("getHeaders route's body is: ${body}, Headers: ${in.headers}")
                .doCatch(Exception.class)
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
                .setHeader("Content-Type", constant("application/json"))
                .setBody(simple("Error processing request: ${exception.message}"))
                .log("Error processing request: ${exception.message}")
                .end();
    }
}
