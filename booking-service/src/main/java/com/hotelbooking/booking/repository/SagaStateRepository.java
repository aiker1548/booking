package com.hotelbooking.booking.repository;

import com.hotelbooking.booking.saga.SagaState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SagaStateRepository extends JpaRepository<SagaState, UUID> {
    Optional<SagaState> findByRequestId(UUID requestId);
    void deleteByRequestId(UUID requestId);
    void deleteAllByStatusIn(Iterable<String> statuses);
}