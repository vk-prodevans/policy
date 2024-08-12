package com.camel.PolicyManagement.model;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "payment_status_table")
@Getter
@Setter
public class PaymentStatus {
    @Id
    private Integer payment_status_id;

    private BigDecimal payment_status_amount;
    private String payment_status;
    private String payment_time_date;

    @ManyToOne
    @JoinColumn(name = "payment_id")
    private Payments payment;

    @ManyToOne
    @JoinColumn(name = "insurance_id")
    private Insurance insurance;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;
}