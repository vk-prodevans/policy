package com.camel.PolicyManagement.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_table")
@Getter
@Setter
public class User {
    @Id
    private Integer user_id;
    private String user_firstname;
    private String user_lastname;
    private String user_email;
    private String user_username;
    private String user_address;
    private String user_gender;
    private String user_role;

}