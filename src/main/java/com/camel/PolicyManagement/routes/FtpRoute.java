package com.camel.PolicyManagement.routes;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.properties.PropertiesComponent;
import org.springframework.stereotype.Component;

@Component
public class FtpRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        PropertiesComponent properties = (PropertiesComponent) getContext().getPropertiesComponent();
        properties.setLocation("classpath:application.properties");

        // FTP server configuration
        String ftpServer = "{{ftp.server}}";
        String ftpPort = "{{ftp.port}}";
        String ftpUsername = "{{ftp.username}}";
        String ftpPassword = "{{ftp.password}}";

        // Upload files to FTP server
        from("file://{{file.image.directory}}?noop=true")
            .log("Uploading file ${file:name} to FTP server")
            // .to("ftp://" + ftpUsername + "@" + ftpServer + ":" + ftpPort + "/{{ftp.upload.directory}}?password=" + ftpPassword)
            .to("ftp://ftpadmin@192.168.95.7?password=Redhat@123&binary=true")
            .log("Uploaded file ${file:name} to FTP server");

       
    }
}
