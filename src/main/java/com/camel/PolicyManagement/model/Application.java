package com.camel.PolicyManagement.model;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "application_table")
@Getter
@Setter
public class Application {
    @Id
    private Integer application_status_id;
    private String application_status;
    private Date application_date;
    private BigDecimal application_monthly_income;
    private BigDecimal application_coverage_amount;     
    private Time application_start_time_duration;
    private Time application_end_time_duration;
    private String insurance_number;

    @ManyToOne
    @JoinColumn(name = "insurance_id")
    private Insurance insurance;
   
}