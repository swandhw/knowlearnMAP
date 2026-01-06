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

    @Column
    private LocalDateTime verificationTokenExpiry;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20) default 'FREE'")
    private Grade grade = Grade.FREE;

    public enum Role {
        USER, ADMIN, SYSOP
    }

    public enum Status {
        VERIFYING_EMAIL,
        WAITING_APPROVAL,
        APPROVED_WAITING_PASSWORD,
        ACTIVE
    }

    public enum Grade {
        FREE(1, 3, 5),
        PRO(3, 10, 20),
        MAX(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

        private final int maxWorkspaces;
        private final int maxDocuments;
        private final int maxPagesPerDocument;

        Grade(int maxWorkspaces, int maxDocuments, int maxPagesPerDocument) {
            this.maxWorkspaces = maxWorkspaces;
            this.maxDocuments = maxDocuments;
            this.maxPagesPerDocument = maxPagesPerDocument;
        }

        public int getMaxWorkspaces() {
            return maxWorkspaces;
        }

        public int getMaxDocuments() {
            return maxDocuments;
        }

        public int getMaxPagesPerDocument() {
            return maxPagesPerDocument;
        }
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
