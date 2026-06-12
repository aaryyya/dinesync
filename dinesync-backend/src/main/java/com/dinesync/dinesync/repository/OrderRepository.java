package com.dinesync.dinesync.repository;

import com.dinesync.dinesync.model.Order;
import com.dinesync.dinesync.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findBySessionUuidOrderByCreatedAtAsc(String sessionUuid);

    List<Order> findByTableIdAndStatus(Integer tableId, OrderStatus status);

    // Used by Phase 4 billing endpoint
    List<Order> findBySessionUuidAndStatus(String sessionUuid, OrderStatus status);
}
