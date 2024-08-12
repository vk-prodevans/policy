package com.camel.PolicyManagement.routes;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.attachment.AttachmentMessage;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.component.sql.SqlComponent;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.beans.factory.annotation.Autowired;

import com.camel.PolicyManagement.util.PdfConverter;

import jakarta.activation.DataHandler;
import jakarta.activation.FileDataSource;

public class IssuePayments extends RouteBuilder {
@Autowired
        PdfConverter pdfConverter;
    @SuppressWarnings("static-access")
@Override
    public void configure() throws Exception {
       PropertiesComponent properties = (PropertiesComponent) getContext().getPropertiesComponent();
                properties.setLocation("classpath:application.properties");
                getContext().getComponent("sql", SqlComponent.class);

                restConfiguration().component("jetty")
                                .port(8084).host("0.0.0.0")
                                .bindingMode(RestBindingMode.json);
                /*
                 * This issuePayments will fetch the details from insurance and policy_plans entity
                 * This route will also helps us to calculate the 
                 */

                from("rest:get:issuePayment/{insuranceId}")
                                .routeId("issuePayments")
                                .log("getPolicyDetails route started at: ${date:now:yyyy-MM-dd HH:mm:ss}")
                                .doTry()
                                //setting the header with insurance id.
                                .process(exchange -> {
                                        String insuranceId = exchange.getIn().getHeader("insuranceId", String.class);
                                        exchange.getIn().setHeader("insuranceId", insuranceId);
                                })
                                //here this is a sql query which will fetch the required feilds to calculate the payments
                                .setBody(simple(
                                                "SELECT i.insurance_premium, i.insurance_amount, i.insurance_type_id, i.insurance_description, pp.* "
                                                                +
                                                                "FROM insurance_table i " +
                                                                "LEFT JOIN policyplans_table pp ON i.insurance_plan_id = pp.plans_id "
                                                                +
                                                                "WHERE i.insurance_id = ${header.insuranceId}"))

                                .to("jdbc:datasource")
                                .process(exchange -> {
                                        String currentMonth = java.time.LocalDate.now().getMonth().toString();
                                        int currentday = java.time.LocalDate.now().getDayOfMonth();
                                        int emiDay = currentday + 5;
                                        @SuppressWarnings("unchecked")
                                        List<Map<String, Object>> policyList = exchange.getIn().getBody(List.class);
                                        if (policyList != null && !policyList.isEmpty()) {
                                                Map<String, Object> policy = policyList.get(0);
                                                exchange.getIn().setHeader("insurance_premium",
                                                                policy.get("insurance_premium"));
                                                exchange.getIn().setHeader("insurance_amount",
                                                                policy.get("insurance_amount"));
                                                exchange.getIn().setHeader("insurance_type_id",
                                                                policy.get("insurance_type_id"));
                                                exchange.getIn().setHeader("insurance_description",
                                                                policy.get("insurance_description"));
                                                exchange.getIn().setHeader("plans_id", policy.get("plans_id"));
                                                exchange.getIn().setHeader("plans_name", policy.get("plans_name"));
                                                exchange.getIn().setHeader("plans_description",
                                                                policy.get("plans_description"));
                                                exchange.getIn().setHeader("plans_interest_rate",
                                                                policy.get("plans_interest_rate"));
                                                exchange.getIn().setHeader("plans_max_amount_issuable",
                                                                policy.get("plans_max_amount_issuable"));
                                                exchange.getIn().setHeader("plans_min_monthly_income",
                                                                policy.get("plans_min_monthly_income"));
                                                BigDecimal minMonthlyIncome = (BigDecimal) policy
                                                                .get("plans_min_monthly_income");
                                                BigDecimal maxAmountIssuable = (BigDecimal) policy
                                                                .get("plans_max_amount_issuable");
                                                BigDecimal maxPayable = maxAmountIssuable
                                                                .multiply(BigDecimal.valueOf(0.66));

                                                BigDecimal fixedMonthlyPayment = minMonthlyIncome
                                                                .multiply(BigDecimal.valueOf(0.10));
                                                String insuranceType = (String) policy.get("plans_type");
                                               
                                                int numberOfMonths = (int) Math
                                                                .ceil(maxPayable.divide(fixedMonthlyPayment)
                                                                                .doubleValue());
                                                exchange.getIn().setHeader("fixedMonthlyPayment", fixedMonthlyPayment);
                                                exchange.getIn().setHeader("numberOfMonths", numberOfMonths);
                                                exchange.getIn().setHeader("insuranceType", insuranceType);
                                                exchange.getIn().setHeader("currentMonth", currentMonth);
                                                exchange.getIn().setHeader("emiDay", emiDay);
                                                
                                                

                                                // Map<String, Object> responseBody = new LinkedHashMap<>();
                                                // responseBody.put("fixedMonthlyPayment", fixedMonthlyPayment);
                                                // responseBody.put("numberOfMonths", numberOfMonths);
                                                // exchange.getMessage().setBody(responseBody);
                                        } else {
                                                throw new RuntimeException(
                                                                "No policy found for the provided insurance ID");
                                        }
                                })
                                .marshal().json()
                                .removeHeaders("*")
                                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                                .log("getPolicyDetails route's body is: ${body}, Headers: ${in.headers}")
                                .doCatch(Exception.class)
                                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
                                .setHeader("Content-Type", constant("application/json"))
                                .setBody(simple("Error processing request: ${exception.message}"))
                                .log("Error processing request: ${exception.message}")
                                .end()

                                //giving the payment details to the html page
                                .to("thymeleaf:templates/policy-template.html")
                                .convertBodyTo(String.class)
                                .setBody(simple("${body.replaceAll('\r', '')} thank you visit again"))
                                .process(exchange -> {
                                        String insuranceType = exchange.getIn().getHeader("insuranceType",
                                                        String.class);
                                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
                                        String currentDate = dateFormat.format(new java.util.Date());
                                        String fileName = insuranceType + "_" + currentDate + ".pdf";
                                        exchange.getIn().setHeader("PolicyFileName", fileName);
                                })
                                .process(exchange -> {
                                        String html = exchange.getIn().getBody(String.class);
                                        String xhtml = pdfConverter.htmlToXhtml(html);
                                        exchange.getIn().setBody(xhtml);
                                })
                             //here creating a pdf using html template
                                .to("direct:htmlToPdf");
                from("direct:htmlToPdf").routeId("pdfroute")
                                .doTry()
                                .log("Converting HTML to PDF")
                                .process(exchange -> {
                                        String xhtml = exchange.getIn().getBody(String.class);
                                        String fileName = exchange.getIn().getHeader("PolicyFileName", String.class);
                                        String filepath = exchange.getContext()
                                                        .resolvePropertyPlaceholders("{{file.image.directory}}");
                                        pdfConverter.xhtmlToPdf(xhtml, filepath, fileName);
                                })
                                .log("PDF file created")
                                // .log("${body}")
                                .to("direct:mailRoute")
                                .removeHeaders("*", "Content-Type")

                                .doCatch(SQLException.class)
                                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
                                .setHeader("Content-Type", constant("application/json"))
                                .setBody(simple("Error processing request: Database error occurred"))
                                .log("SQLException occurred: ${exception.message}")

                                .doCatch(IllegalArgumentException.class)
                                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
                                .setHeader("Content-Type", constant("application/json"))
                                .setBody(simple("Error processing request: ${exception.message}"))
                                .log("IllegalArgumentException occurred: ${exception.message}")
                                .end();
                //sending the email to the user email with attatched pdf
                from("direct:mailRoute")
                                .routeId("sendEmail")
                                .process(exchange -> {

                                        // String userName = exchange.getIn().getHeader("userName", String.class);

                                        String fileName = exchange.getIn().getHeader("PolicyFileName", String.class);
                                        String filePath = exchange.getContext()
                                                        .resolvePropertyPlaceholders("{{file.image.directory}}");
                                        // setting body content for email
                                        String textContent = "Hello " + "userName" + ","
                                                        + "Your policy document is attached.\n\n" +
                                                        "Thank you.\n\n" +
                                                        "Regards,\n" +
                                                        "Policy Issuer";

                                        exchange.getIn().setBody(textContent);
                                        AttachmentMessage attachmentMessage = exchange.getIn(AttachmentMessage.class);
                                        attachmentMessage.addAttachment(fileName,
                                                        new DataHandler(new FileDataSource(
                                                                        filePath + fileName)));
                                })
                                .log("Sending email...")
                                .setHeader("Subject", constant("you can find the policy payment documents below"))
                                .setHeader("To", simple("jalagam220@gmail.com"))
                                .setHeader("From", constant("${spring.mail.username}"))
                                .to("smtps://{{spring.mail.host}}:{{spring.mail.port}}?username={{spring.mail.username}}&password={{spring.mail.password}}&mail.smtp.auth=true&mail.smtp.starttls.enable=true")
                                .log("Email sent successfully to ${header.To}")

                                .setHeader("Content-Type", constant("application/json"))
                                .marshal().json()
                                .setBody(simple("{\"success\": true, \"message\": \"Email sent with file attached.\"}"));
    }
    
}
