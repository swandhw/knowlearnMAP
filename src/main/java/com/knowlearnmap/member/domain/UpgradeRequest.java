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
@Table(name = "upgrade_request")
public class UpgradeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestType requestType;

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false)
    private String contactName;

    @Column(nullable = false)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String documentPaths; // Comma separated paths for simplicity

    @Column
    private LocalDateTime meetingDateTime; // For MAX consultation

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public enum RequestType {
        PRO_UPGRADE, MAX_CONSULTATION
    }

    public enum RequestStatus {
        PENDING, APPROVED, REJECTED
    }

    public UpgradeRequest(Long memberId, RequestType requestType, String companyName, String contactName, String phone,
            String documentPaths, LocalDateTime meetingDateTime) {
        this.memberId = memberId;
        this.requestType = requestType;
        this.companyName = companyName;
        this.contactName = contactName;
        this.phone = phone;
        this.documentPaths = documentPaths;
        this.meetingDateTime = meetingDateTime;
    }
}
