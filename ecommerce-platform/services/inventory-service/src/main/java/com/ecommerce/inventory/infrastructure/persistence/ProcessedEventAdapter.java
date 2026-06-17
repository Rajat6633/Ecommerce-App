package com.ecommerce.inventory.infrastructure.persistence;

import com.ecommerce.inventory.application.port.out.ProcessedEventPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;

@Component
public class ProcessedEventAdapter implements ProcessedEventPort {

    private final ProcessedEventJpaRepository repository;
    private final Clock clock;

    public ProcessedEventAdapter(ProcessedEventJpaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public boolean firstSeen(UUID eventId) {
        if (repository.existsById(eventId)) {
            return false;
        }
        try {
            repository.save(new ProcessedEventEntity(eventId, clock.instant()));
            return true;
        } catch (DataIntegrityViolationException duplicate) {
            // Concurrent insert of the same event id — treat as already seen.
            return false;
        }
    }
}
