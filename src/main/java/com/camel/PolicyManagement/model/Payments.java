package com.camel.PolicyManagement.model;

import java.math.BigDecimal;
import java.util.List;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "payments_table")
@Getter
@Setter
public class Payments {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "insurance_id_seq")
    @SequenceGenerator(name = "insurance_id_seq", sequenceName = "insurance_id_seq", allocationSize = 1)
    private Integer payment_id;
    private Integer payment_policy_id;
    private String payment_date;
    private BigDecimal payment_amount;
    private String payment_description;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL)
    private List<PaymentStatus> paymentStatuses;

    @ManyToOne
    @JoinColumn(name = "insurance_id")
    private Insurance insurance;

}