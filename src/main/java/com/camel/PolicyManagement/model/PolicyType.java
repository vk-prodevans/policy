package com.camel.PolicyManagement.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "policy_type_table")
@Getter
@Setter
public class PolicyType {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "policytype_id_seq")
    @SequenceGenerator(name = "policytype_id_seq", sequenceName = "policytype_id_seq", allocationSize = 1)
    private Integer policytype_id;

    private String policytype_name;

    private String policytype_type;

    private BigDecimal policytype_interest_rate;

    private BigDecimal policytype_max_amount_issuable;

    private BigDecimal policytype_min_monthly_income;
}