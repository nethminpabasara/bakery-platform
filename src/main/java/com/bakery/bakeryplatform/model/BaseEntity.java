package com.bakery.bakeryplatform.model;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import java.time.LocalDateTime;

@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() {

        return id;
    }

    public LocalDateTime getCreatedAt() {

        return createdAt;
    }

    // every child class must write its own version of this method
    public abstract String describe();
}