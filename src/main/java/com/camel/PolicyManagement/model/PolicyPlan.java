package com.camel.PolicyManagement.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


import java.math.BigDecimal;

@Entity
@Table(name = "policyplans_table")
@Getter
@Setter
public class PolicyPlan {
    @Id
    private Integer plans_id;
    private String plans_name;
    private String plans_type;
    private String plans_description;
    private BigDecimal plans_interest_rate;
    private BigDecimal plans_max_amount_issuable;
    private BigDecimal plans_min_monthly_income;
    
    @ManyToOne
    @JoinColumn(name = "insurance_id") 
    private Insurance insurance;
    
}
