package com.camel.PolicyManagement.model;

import java.util.List;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

//this is the model class for Customer entity
@Entity
@Table(name = "customer_table")
@Getter
@Setter
public class Customer {
    @Id
    private String user_id;
    private String customer_firstname;
    private String customer_lastname;
    private String customer_email;
    private String customer_dob;
    private String customer_username;
    private String customer_address;
    private String customer_gender;
    private String customer_role;
  

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private List<Insurance> insurances;

    
}