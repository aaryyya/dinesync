package com.dinesync.dinesync.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_uuid", nullable = false, unique = true, length = 36)
    private String sessionUuid;

    @Column(name = "table_id", nullable = false)
    private Integer tableId;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;
}
