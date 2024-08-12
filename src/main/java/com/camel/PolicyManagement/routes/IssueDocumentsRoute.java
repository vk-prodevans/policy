package com.camel.PolicyManagement.routes;

import java.io.File;
import java.io.IOException;
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
import org.apache.camel.model.dataformat.BarcodeDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.beans.factory.annotation.Autowired;

import com.camel.PolicyManagement.util.PdfConverter;

import jakarta.activation.DataHandler;
import jakarta.activation.FileDataSource;

public class IssueDocumentsRoute extends RouteBuilder {
    @Autowired
    PdfConverter pdfConverter;

    @Override
    public void configure() throws Exception {
        PropertiesComponent properties = (PropertiesComponent) getContext().getPropertiesComponent();
        properties.setLocation("classpath:application.properties");
        getContext().getComponent("sql", SqlComponent.class);

        restConfiguration().component("jetty")
                .port(8084).host("0.0.0.0")
                .bindingMode(RestBindingMode.json);

        BarcodeDataFormat barcodeDataFormat = new BarcodeDataFormat();
        barcodeDataFormat.setBarcodeFormat("QR-Code");
        barcodeDataFormat.setImageType("png");
        barcodeDataFormat.setWidth("200");
        barcodeDataFormat.setHeight("200");

        /*
         * This route will issue the documents to the particular user through an email
         * 
         */
        from("rest:get:issueDocuments/{insuranceId}")
                .routeId("issueDocuments")
                .setHeader("Access-Control-Allow-Credentials", constant("true"))
                .setHeader("Access-Control-Allow-Origin", constant("*"))
                .setHeader("Access-Control-Allow-Methods", constant("GET, POST, OPTIONS, PUT, DELETE, HEAD"))
                .setHeader("Access-Control-Allow-Headers", constant(
                        "Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers"))
                .choice()
                .when(header(Exchange.HTTP_METHOD).isEqualTo("OPTIONS"))
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(204))
                .setBody(constant(""))
                .endChoice()
                .otherwise()
                .log("issueDocument route started at: ${date:now:yyyy-MM-dd HH:mm:ss}")
                .doTry()
                .process(exchange -> {
                    String insuranceId = exchange.getIn().getHeader("insuranceId", String.class);
                    exchange.getIn().setHeader("insuranceId", insuranceId);
                })
                .setBody(simple(
                        "SELECT i.insurance_premium, i.insurance_amount, i.insurance_plan, a.*, " +
                                "c.user_id, c.customer_firstname, c.customer_lastname, c.customer_email, c.customer_dob, "
                                +
                                "c.customer_username, c.customer_address, c.customer_gender, c.customer_role " +
                                "FROM insurance_table i " +
                                "LEFT JOIN application_table a ON i.insurance_id = a.insurance_id " +
                                "LEFT JOIN customer_table c ON i.user_id = c.user_id " +
                                "WHERE i.insurance_id = ${header.insuranceId}"))
                .to("jdbc:datasource")
                .process(exchange -> {
                    String currentMonth = java.time.LocalDate.now().getMonth().toString();
                    int currentday = java.time.LocalDate.now().getDayOfMonth();
                    int emiDay = currentday + 5;
                    int payment_date = currentday + 5;
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> resultList = exchange.getIn().getBody(List.class);
                    if (resultList != null && !resultList.isEmpty()) {
                        Map<String, Object> resultMap = resultList.get(0);
                        exchange.getIn().setHeader("insurance_premium", resultMap.get("insurance_premium"));
                        exchange.getIn().setHeader("insurance_amount", resultMap.get("insurance_amount"));
                        exchange.getIn().setHeader("insurance_plan", resultMap.get("insurance_plan"));
                        exchange.getIn().setHeader("application_status_id", resultMap.get("application_status_id"));
                        exchange.getIn().setHeader("application_status", resultMap.get("application_status"));
                        exchange.getIn().setHeader("application_date", resultMap.get("application_date"));
                        exchange.getIn().setHeader("application_monthly_income",
                                resultMap.get("application_monthly_income"));
                        exchange.getIn().setHeader("application_coverage_amount",
                                resultMap.get("application_coverage_amount"));
                        exchange.getIn().setHeader("application_start_time_duration",
                                resultMap.get("application_start_time_duration"));
                        exchange.getIn().setHeader("application_end_time_duration",
                                resultMap.get("application_end_time_duration"));
                        exchange.getIn().setHeader("insurance_number", resultMap.get("insurance_number"));

                        exchange.getIn().setHeader("customer_firstname", resultMap.get("customer_firstname"));
                        exchange.getIn().setHeader("customer_lastname", resultMap.get("customer_lastname"));
                        exchange.getIn().setHeader("customer_email", resultMap.get("customer_email"));
                        exchange.getIn().setHeader("customer_dob", resultMap.get("customer_dob"));
                        exchange.getIn().setHeader("customer_username", resultMap.get("customer_username"));
                        exchange.getIn().setHeader("customer_address", resultMap.get("customer_address"));
                        exchange.getIn().setHeader("customer_gender", resultMap.get("customer_gender"));
                        exchange.getIn().setHeader("customer_role", resultMap.get("customer_role"));
                        exchange.getIn().setHeader("user_id", resultMap.get("user_id"));
                        BigDecimal minMonthlyIncome = (BigDecimal) resultMap.get("application_monthly_income");
                        BigDecimal maxPayable = (BigDecimal) resultMap.get("application_coverage_amount");
                        BigDecimal payment_ammount = (BigDecimal) resultMap.get("application_coverage_amount");
                        String payment_description = (String) resultMap.get("insurance_plan");
                        String user_id = (String) resultMap.get("user_id");
                        Integer insurance_id = (Integer) resultMap.get("insurance_id");
                        // BigDecimal maxPayable = maxAmountIssuable.multiply(BigDecimal.valueOf(0.66));

                        BigDecimal fixedMonthlyPayment = minMonthlyIncome.multiply(BigDecimal.valueOf(0.10));
                        int numberOfMonths = (int) Math
                                .ceil(maxPayable.divide(fixedMonthlyPayment, BigDecimal.ROUND_CEILING).doubleValue());
                        exchange.getIn().setHeader("user_id", user_id);
                        exchange.getIn().setHeader("insurance_id", insurance_id);
                        exchange.getIn().setHeader("fixedMonthlyPayment", fixedMonthlyPayment);
                        exchange.getIn().setHeader("numberOfMonths", numberOfMonths);
                        exchange.getIn().setHeader("currentMonth", currentMonth);
                        exchange.getIn().setHeader("emiDay", emiDay);
                        exchange.getIn().setHeader("payment_date", payment_date);
                        exchange.getIn().setHeader("payment_ammount", payment_ammount);
                        exchange.getIn().setHeader("payment_description", payment_description);
                        exchange.getIn().setHeader("minMonthlyIncome", minMonthlyIncome);
                    } else {
                        throw new RuntimeException("No policy found for the provided insurance ID");
                    }
                })
                .marshal().json()
                .to("seda:addpayments")
                .to("seda:processPolicy")

                .end()
                .endChoice();
        /*
         * this route is responsible for inserting the calculated payments to the
         * payment table
         */
        from("seda:addpayments")
                .log("Processing payment insertion")
                .log("body of payments ${body}")
                .unmarshal().json(JsonLibrary.Jackson, List.class)
                .process(exchange -> {
                    String currentMonth = java.time.LocalDate.now().getMonth().toString();
                    int currentday = java.time.LocalDate.now().getDayOfMonth();
                    int emiDay = currentday + 5;
                    int payment_date = currentday + 5;
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> payments = exchange.getIn().getBody(List.class);
                    if (payments == null || payments.isEmpty()) {
                        throw new IllegalArgumentException("Request body is null or empty");
                    }

                    // Process the first payment only
                    Map<String, Object> body = payments.get(0);

                    // Extract values from the map, handling nulls
                    BigDecimal minMonthlyIncome = BigDecimal.valueOf(((Double) body.get("application_monthly_income")));
                    BigDecimal paymentAmount = BigDecimal.valueOf(((Double) body.get("application_coverage_amount")));
                    String paymentDescription = (String) body.getOrDefault("insurance_plan", "");
                    Integer insuranceId = (Integer) body.getOrDefault("insurance_id", 0);
                    String userId = (String) body.getOrDefault("user_id", "");

                    String combinedQuery = String.format(
                            "INSERT INTO payments_table (minMontly_income, payment_date, payment_amount, payment_description, insurance_id, user_id) "
                                    +
                                    "VALUES (%s, %d, %s, '%s', %d, '%s'); " +
                                    "UPDATE application_table SET application_status = 'Approved' WHERE insurance_id = %d",
                            minMonthlyIncome, payment_date, paymentAmount, paymentDescription, insuranceId, userId,
                            insuranceId);

                    exchange.getIn().setBody(combinedQuery);
                })
                .to("jdbc:datasource");
        /*
         * This route will process the details of the insurance and those details will
         * be
         * given to the qr-code generator end point which will generate the qr-code with
         * te desired details and that qr will store in a particular folder.
         */
        from("seda:processPolicy")
                .routeId("processPolicy")
                .log("Processing policy from direct route")
                .process(exchange -> {
                    String customerUsername = exchange.getIn().getHeader("customer_username", String.class);
                    String customerEmail = exchange.getIn().getHeader("customer_email", String.class);
                    BigDecimal application_coverage_amount = exchange.getIn().getHeader("application_coverage_amount",
                            BigDecimal.class);
                    String textToEncode = "Username: " + customerUsername + ", Email: " + customerEmail
                            + ", Max Amount Issuable: " + application_coverage_amount;
                    exchange.getIn().setBody(textToEncode);
                })
                .marshal(barcodeDataFormat)
                .process(exchange -> {
                    String customerUsername = exchange.getIn().getHeader("customer_username", String.class);
                    String fileName = "QR_" + customerUsername + ".png";
                    String folderPath = "/home/hp/Downloads/PolicyManagement/QRCode/";
                    String filePath = folderPath + fileName;
                    exchange.getIn().setHeader("QRCodeFileName", fileName);
                    exchange.getIn().setHeader("qrCodePath", filePath);
                    exchange.getIn().setHeader(Exchange.FILE_NAME, fileName);
                    exchange.getIn().setBody(exchange.getIn().getBody(byte[].class));
                })
                .to("file:///home/hp/Downloads/PolicyManagement/QRCode")
                .process(exchange -> {
                    String plans_name = exchange.getIn().getHeader("plans_name", String.class);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
                    String currentDate = dateFormat.format(new java.util.Date());
                    String fileName = (plans_name != null ? plans_name : "Policy") + "_" + currentDate + ".pdf";
                    exchange.getIn().setHeader("PolicyFileName", fileName);
                    log.info("PolicyFileName set to: " + fileName);
                    String qrCodePath = exchange.getIn().getHeader("qrCodePath", String.class);
                    exchange.getIn().setHeader("qrCodePath", qrCodePath); // Set the path for Thymeleaf
                })
                .to("thymeleaf:templates/policy-template.html")
                .convertBodyTo(String.class)
                .process(exchange -> {
                    String html = exchange.getIn().getBody(String.class);
                    String xhtml = PdfConverter.htmlToXhtml(html);
                    exchange.getIn().setBody(xhtml);
                })
                .to("seda:htmlToPdf");
        /*
         * this route here will convert the thymleaf html template to the pdf.
         * This pdf generated will be stored in a folder
         */
        from("seda:htmlToPdf")
                .routeId("pdfroute")
                .doTry()
                .log("Converting HTML to PDF")
                .process(exchange -> {
                    String xhtml = exchange.getIn().getBody(String.class);
                    String fileName = exchange.getIn().getHeader("PolicyFileName", String.class);
                    if (fileName == null) {
                        throw new IllegalArgumentException("PolicyFileName header is null");
                    }
                    String filepath = exchange.getContext().resolvePropertyPlaceholders("{{file.image.directory}}");
                    log.info("Converting to PDF: " + fileName + " at " + filepath);
                    PdfConverter.xhtmlToPdf(xhtml, filepath, fileName);

                    // Set the FilePath and FileName headers for the FTP transfer
                    exchange.getIn().setHeader("FilePath", filepath + fileName);
                    exchange.getIn().setHeader("FileName", fileName);
                })
                .log("PDF file created")
                .log("Transferring PDF to FTP server")
                .process(exchange -> {
                    String fileName = exchange.getIn().getHeader("FileName", String.class);
                    String filePath = exchange.getIn().getHeader("FilePath", String.class);

                    // Log the file path and name for debugging
                    log.info("FilePath: " + filePath);
                    log.info("FileName: " + fileName);

                    File file = new File(filePath);
                    if (file.exists()) {
                        exchange.getIn().setBody(file);
                        exchange.getIn().setHeader(Exchange.FILE_NAME, fileName);
                    } else {
                        throw new IOException("File not found: " + filePath);
                    }
                })
                .to("ftp://ftpuser@ftp-service-keyclock.apps.cluster-fkjxv.fkjxv.sandbox2233.opentlc.com?password=ftppassword&binary=true")
                .log("File transferred to FTP server successfully")
                .to("seda:mailRoute")
                .removeHeaders("*", "Content-Type");

        /*
         * This route will process the details and attach the pdf generated with qr-code
         * inserted
         * and will generate an email and will send that email to the paticular user who
         * requested
         * the insurance
         */
        from("seda:mailRoute")
                .routeId("sendEmail")
                .doTry()
                .process(exchange -> {
                    String fileName = exchange.getIn().getHeader("PolicyFileName", String.class);
                    if (fileName == null) {
                        throw new IllegalArgumentException("PolicyFileName header is null");
                    }
                    String filePath = exchange.getContext().resolvePropertyPlaceholders("{{file.image.directory}}")
                            + fileName;
                    File policyFile = new File(filePath);
                    AttachmentMessage attachmentMessage = exchange.getIn(AttachmentMessage.class);
                    attachmentMessage.addAttachment(fileName, new DataHandler(new FileDataSource(policyFile)));
                    exchange.getIn().setBody("Find attached your policy document");
                })
                .log("Sending email...")
                .setHeader("Subject", constant("You can find the policy payment documents below"))
                .setHeader("To", header("customer_email"))
                .setHeader("From", constant("${spring.mail.username}"))
                .to("smtps://{{spring.mail.host}}:{{spring.mail.port}}?username={{spring.mail.username}}&password={{spring.mail.password}}&mail.smtp.auth=true&mail.smtp.starttls.enable=true")
                .log("Email sent successfully to ${header.To}")
                .setHeader("Content-Type", constant("application/json"))
                .marshal().json()
                .setBody(simple("{\"success\": true, \"message\": \"Email sent with file attached.\"}"))
                .doCatch(IOException.class)
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
                .setHeader("Content-Type", constant("application/json"))
                .setBody(
                        simple("{\"success\": false, \"message\": \"Error processing request: ${exception.message}\"}"))
                .log("IOException occurred: ${exception.message}")
                .doCatch(Exception.class)
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
                .setHeader("Content-Type", constant("application/json"))
                .setBody(
                        simple("{\"success\": false, \"message\": \"Error processing request: ${exception.message}\"}"))
                .log("Exception occurred: ${exception.message}")
                .end()
                .removeHeaders("*", "Content-Type");

    }

    

    

}
