package com.ecommerce.inventory.application.service;

import com.ecommerce.inventory.application.port.out.InventoryRepositoryPort;
import com.ecommerce.inventory.domain.exception.InventoryItemNotFoundException;
import com.ecommerce.inventory.domain.model.InventoryItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock InventoryRepositoryPort inventoryRepository;

    private InventoryService service;
    private final UUID productId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new InventoryService(inventoryRepository);
    }

    @Test
    void getByProduct_missing_throwsNotFound() {
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getByProduct(productId))
                .isInstanceOf(InventoryItemNotFoundException.class);
    }

    @Test
    void upsertStock_newProduct_createsItem() {
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.empty());
        when(inventoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        InventoryItem result = service.upsertStock(productId, 50, 5);

        assertThat(result.onHand()).isEqualTo(50);
        assertThat(result.reorderLevel()).isEqualTo(5);
        assertThat(result.reserved()).isZero();
    }

    @Test
    void upsertStock_existing_updatesOnHandKeepsReserved() {
        InventoryItem existing = new InventoryItem(UUID.randomUUID(), productId, 10, 3, 2, 7L);
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(existing));
        when(inventoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        InventoryItem result = service.upsertStock(productId, 100, 9);

        assertThat(result.onHand()).isEqualTo(100);
        assertThat(result.reorderLevel()).isEqualTo(9);
        assertThat(result.reserved()).isEqualTo(3);     // preserved
        assertThat(result.version()).isEqualTo(7L);     // preserved for optimistic lock
    }

    @Test
    void receiveStock_addsToOnHand() {
        InventoryItem existing = new InventoryItem(UUID.randomUUID(), productId, 10, 0, 0, 1L);
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(existing));
        when(inventoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertThat(service.receiveStock(productId, 15).onHand()).isEqualTo(25);
    }

    @Test
    void receiveStock_missing_throwsNotFound() {
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.receiveStock(productId, 5))
                .isInstanceOf(InventoryItemNotFoundException.class);
    }
}
