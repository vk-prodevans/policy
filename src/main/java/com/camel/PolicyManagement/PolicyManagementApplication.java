package com.camel.PolicyManagement;

import javax.sql.DataSource;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultRegistry;
import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;


import com.camel.PolicyManagement.routes.CheckPolicyStatusRoute;
import com.camel.PolicyManagement.routes.CustomerRoute;
import com.camel.PolicyManagement.routes.FetchPaymentAmountRoute;
import com.camel.PolicyManagement.routes.FtpRoute;
import com.camel.PolicyManagement.routes.GetAllPoliciesRoute;
import com.camel.PolicyManagement.routes.GetHeaders;
import com.camel.PolicyManagement.routes.GetPayments;
import com.camel.PolicyManagement.routes.GetPoliciesRoute;
import com.camel.PolicyManagement.routes.InsuranceRoute;
import com.camel.PolicyManagement.routes.IssueDocumentsRoute;
import com.camel.PolicyManagement.routes.IssuePayments;
import com.camel.PolicyManagement.routes.MakePaymentRoute;
import com.camel.PolicyManagement.routes.PaymentCalculationRoute;
import com.camel.PolicyManagement.routes.PolicyPremium;
import com.camel.PolicyManagement.routes.RejectPolicyRoute;
import com.camel.PolicyManagement.routes.RejectedPolicies;
import com.camel.PolicyManagement.routes.UpdatePolicy;
import com.camel.PolicyManagement.routes.VerifiedPolicyRoute;
import com.camel.PolicyManagement.routes.VerifyPaymentDetailsRoute;

import jakarta.jms.ConnectionFactory;


@SpringBootApplication
public class PolicyManagementApplication {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(PolicyManagementApplication.class, args);

    }
  
    //bean for the datasorce
	@Bean
    @ConfigurationProperties(prefix = "spring.datasource")
	public DataSource dataSource() {
		return new BasicDataSource();
	}
    @Bean
    public ConnectionFactory connectionFactory() {
        return new ActiveMQConnectionFactory("admin", "admin", "tcp://localhost:61616");
    }

	 @Bean
    public CamelContext camelContext(DataSource dataSource, ConnectionFactory connectionFactory) throws Exception {
        DefaultRegistry reg = new DefaultRegistry();
        reg.bind("datasource", dataSource);
        CamelContext context = new DefaultCamelContext(reg);
        context.addComponent("jms", JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));

       

        context.addRoutes(new CheckPolicyStatusRoute());
        context.addRoutes(new CustomerRoute());
        context.addRoutes(new FetchPaymentAmountRoute());
        // context.addRoutes(new FtpRoute());
        context.addRoutes(new GetAllPoliciesRoute());
        context.addRoutes(new GetPayments());
        context.addRoutes(new InsuranceRoute());
        context.addRoutes(new IssuePayments());
        context.addRoutes(new MakePaymentRoute());
        context.addRoutes(new PaymentCalculationRoute());
        context.addRoutes(new PolicyPremium());
        context.addRoutes(new RejectedPolicies());
        context.addRoutes(new VerifiedPolicyRoute());
        context.addRoutes(new VerifyPaymentDetailsRoute());
        context.addRoutes(new IssueDocumentsRoute());
        context.addRoutes(new GetHeaders());
        context.addRoutes(new GetPoliciesRoute());
        context.addRoutes(new RejectPolicyRoute());
        context.addRoutes(new UpdatePolicy());

        //context started
        context.start();
        return context;
    }

}
