package com.camel.PolicyManagement.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "headers")
@Getter
@Setter
public class Headers {

    @Id
    private String header_id;
    private String application_status;
    private String application_date;
    private String insurance_expire_date;
    private String insurance_plan;
    private String insurance_issue_date;
    private String insurance_number;
    private String customer_firstname;
    private String table_name;
    private String customer_download;
    private String customer_details;
    @Column(name = "selec", length = 50)
    private String selec;

}
