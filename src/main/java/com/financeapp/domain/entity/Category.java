package com.financeapp.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.financeapp.domain.enums.CategoryType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CategoryType type;

    @Column(length = 50)
    private String icon;

    @Column(length = 20)
    private String color;

    @Column(precision = 15, scale = 2)
    private BigDecimal budget;
}