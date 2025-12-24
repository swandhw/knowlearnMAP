package com.knowlearnmap.domain.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "domains")
@Getter
@Setter
@NoArgsConstructor
public class DomainEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "arango_db_name", length = 100)
    private String arangoDbName;

    @Column(length = 1000)
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public DomainEntity(String name, String arangoDbName) {
        this.name = name;
        this.arangoDbName = arangoDbName;
    }
}
