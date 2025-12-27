package com.knowlearnmap.member.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column
    private String verificationToken;

    @Column
    private String domain;

    public enum Role {
        USER, ADMIN, SYSOP
    }

    public enum Status {
        VERIFYING_EMAIL,
        WAITING_APPROVAL,
        APPROVED_WAITING_PASSWORD,
        ACTIVE
    }

    public Member(String email, String password, Role role, String verificationToken, String domain) {
        this.email = email;
        this.password = password;
        this.role = role;
        this.status = Status.VERIFYING_EMAIL;
        this.verificationToken = verificationToken;
        this.domain = domain;
    }
}
