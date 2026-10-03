package com.dev.stockanalyticsbackend.entity;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", length = 100, nullable = false, unique = true)
    private String email;

    @Column(name = "line_notify_token", length = 100)
    private String lineNotifyToken;

    public UserEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLineNotifyToken() {
        return lineNotifyToken;
    }

    public void setLineNotifyToken(String lineNotifyToken) {
        this.lineNotifyToken = lineNotifyToken;
    }
}
