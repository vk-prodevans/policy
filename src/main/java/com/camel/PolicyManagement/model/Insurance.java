
package com.camel.PolicyManagement.model;

import java.util.List;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "insurance_table")
@Getter
@Setter
public class Insurance {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "insurance_id_seq")
    @SequenceGenerator(name = "insurance_id_seq", sequenceName = "insurance_id_seq", allocationSize = 1)
    private Integer insurance_id;
    private String insurance_plan;
    private String insurance_number;
    private String insurance_issue_date;
    private String monthly_income;
    private String insurance_expire_date;
    private BigDecimal insurance_premium;
    private BigDecimal insurance_amount;

    @OneToMany(mappedBy = "insurance", cascade = CascadeType.ALL)
    private List<Application> applications;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private Customer customer;
    @OneToMany(mappedBy = "insurance", cascade = CascadeType.ALL)
    private List<PolicyPlan> policyPlans;

}
