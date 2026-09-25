package br.com.amigo.biblioteca.model;
import jakarta.persistence.*;
@Entity @Table(name="users")
public class User {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private String name;
 @Column(nullable=false,unique=true) private String email;
 @Column(nullable=false) private String passwordHash;
 @Column(nullable=false) private String role;
 private String course;
 public User(){}
 public User(String name,String email,String passwordHash,String role,String course){this.name=name;this.email=email;this.passwordHash=passwordHash;this.role=role;this.course=course;}
 public Long getId(){return id;} public String getName(){return name;} public String getEmail(){return email;} public String getPasswordHash(){return passwordHash;} public String getRole(){return role;} public String getCourse(){return course;}
 public void setId(Long v){id=v;} public void setName(String v){name=v;} public void setEmail(String v){email=v;} public void setPasswordHash(String v){passwordHash=v;} public void setRole(String v){role=v;} public void setCourse(String v){course=v;}
}