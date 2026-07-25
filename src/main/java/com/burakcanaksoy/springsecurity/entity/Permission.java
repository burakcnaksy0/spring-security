package com.burakcanaksoy.springsecurity.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permission")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;
}
