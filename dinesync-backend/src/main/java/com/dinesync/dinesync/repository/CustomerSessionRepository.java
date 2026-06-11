package com.dinesync.dinesync.repository;

import com.dinesync.dinesync.model.CustomerSession;
import com.dinesync.dinesync.model.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerSessionRepository extends JpaRepository<CustomerSession, Long> {

    Optional<CustomerSession> findBySessionUuid(String sessionUuid);

    Optional<CustomerSession> findBySessionUuidAndStatus(String sessionUuid, SessionStatus status);

    List<CustomerSession> findByStatus(SessionStatus status);

    /**
     * Single-query activity bump — avoids the SELECT + UPDATE double round-trip.
     * W4 fix: replaces the findBySessionUuid().ifPresent(save()) pattern.
     */
    @Modifying
    @Transactional
    @Query("UPDATE CustomerSession s SET s.lastActivityAt = :ts WHERE s.sessionUuid = :uuid")
    void touchLastActivity(@Param("uuid") String uuid, @Param("ts") LocalDateTime ts);

    // Used by Phase 4 scheduler to expire idle sessions
    @Modifying
    @Transactional
    @Query("UPDATE CustomerSession s SET s.status = :newStatus " +
           "WHERE s.status = :oldStatus AND s.lastActivityAt < :cutoff")
    int expireIdleSessions(
        @Param("oldStatus") SessionStatus oldStatus,
        @Param("newStatus") SessionStatus newStatus,
        @Param("cutoff") LocalDateTime cutoff
    );
}
