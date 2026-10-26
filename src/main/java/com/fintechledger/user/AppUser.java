package com.fintechledger.user;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="app_users")
public class AppUser {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @Column(nullable=false,unique=true) private String email;
 @Column(name="password_hash",nullable=false) private String passwordHash;
 @Column(name="full_name",nullable=false) private String fullName;
 @Column(nullable=false) private String role="USER";
 @Column(name="created_at",nullable=false) private Instant createdAt=Instant.now();
 protected AppUser() {}
 public AppUser(String email,String passwordHash,String fullName){this.email=email.toLowerCase();this.passwordHash=passwordHash;this.fullName=fullName;}
 public UUID getId(){return id;} public String getEmail(){return email;} public String getPasswordHash(){return passwordHash;} public String getFullName(){return fullName;} public String getRole(){return role;}
}
