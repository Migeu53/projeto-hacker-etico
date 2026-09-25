package br.com.amigo.biblioteca.model;
import jakarta.persistence.*;
@Entity @Table(name="loans")
public class Loan {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Long userId;
 @Column(nullable=false) private String userName;
 @Column(nullable=false) private String bookTitle;
 @Column(nullable=false) private String status;
 private String privateNote;
 public Loan(){}
 public Loan(Long userId,String userName,String bookTitle,String status,String privateNote){this.userId=userId;this.userName=userName;this.bookTitle=bookTitle;this.status=status;this.privateNote=privateNote;}
 public Long getId(){return id;} public Long getUserId(){return userId;} public String getUserName(){return userName;} public String getBookTitle(){return bookTitle;} public String getStatus(){return status;} public String getPrivateNote(){return privateNote;}
 public void setId(Long v){id=v;} public void setUserId(Long v){userId=v;} public void setUserName(String v){userName=v;} public void setBookTitle(String v){bookTitle=v;} public void setStatus(String v){status=v;} public void setPrivateNote(String v){privateNote=v;}
}