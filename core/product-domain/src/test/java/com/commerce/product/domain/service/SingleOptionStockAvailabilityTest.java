package com.commerce.product.domain.service;

import com.commerce.common.event.DomainEventPublisher;
import com.commerce.product.domain.repository.InventoryRepository;
import com.commerce.product.domain.repository.LockRepository;
import com.commerce.product.domain.repository.ProductRepository;
import com.commerce.product.domain.repository.SagaRepository;
import com.commerce.product.domain.service.impl.StockAvailabilityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("단일 옵션 재고 확인 로직 테스트")
class SingleOptionStockAvailabilityTest {

    @Mock
    private InventoryRepository inventoryRepository;
    
    @Mock
    private LockRepository lockRepository;
    
    @Mock
    private ProductRepository productRepository;
    
    @Mock
    private SagaRepository sagaRepository;
    
    @Mock
    private DomainEventPublisher eventPublisher;
    
    private StockAvailabilityService stockAvailabilityService;
    
    @BeforeEach
    void setUp() {
        stockAvailabilityService = new StockAvailabilityServiceImpl(inventoryRepository, productRepository, lockRepository, sagaRepository, eventPublisher);
    }
    
    @Nested
    @DisplayName("checkSingleOption 메서드는")
    class CheckSingleOption {
        
        @Test
        @DisplayName("재고가 요청 수량과 정확히 일치할 때 true를 반환한다")
        void exactMatch() {
            // Given
            String skuId = "SKU001";
            int requestedQuantity = 10;
            when(inventoryRepository.getAvailableQuantity(skuId))
                    .thenReturn(10);
            
            // When
            boolean result = stockAvailabilityService.checkSingleOption(skuId, requestedQuantity);
            
            // Then
            assertThat(result).isTrue();
            verify(inventoryRepository).getAvailableQuantity(skuId);
        }
        
        @Test
        @DisplayName("재고가 요청 수량보다 많을 때 true를 반환한다")
        void moreThanRequested() {
            // Given
            String skuId = "SKU001";
            int requestedQuantity = 10;
            when(inventoryRepository.getAvailableQuantity(skuId))
                    .thenReturn(100);
            
            // When
            boolean result = stockAvailabilityService.checkSingleOption(skuId, requestedQuantity);
            
            // Then
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("재고가 요청 수량보다 적을 때 false를 반환한다")
        void lessThanRequested() {
            // Given
            String skuId = "SKU001";
            int requestedQuantity = 10;
            when(inventoryRepository.getAvailableQuantity(skuId))
                    .thenReturn(5);
            
            // When
            boolean result = stockAvailabilityService.checkSingleOption(skuId, requestedQuantity);
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("재고가 0일 때 false를 반환한다")
        void zeroStock() {
            // Given
            String skuId = "SKU001";
            int requestedQuantity = 1;
            when(inventoryRepository.getAvailableQuantity(skuId))
                    .thenReturn(0);
            
            // When
            boolean result = stockAvailabilityService.checkSingleOption(skuId, requestedQuantity);
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("요청 수량이 0일 때 true를 반환한다")
        void zeroRequested() {
            // Given
            String skuId = "SKU001";
            int requestedQuantity = 0;
            
            // When
            boolean result = stockAvailabilityService.checkSingleOption(skuId, requestedQuantity);
            
            // Then
            assertThat(result).isTrue();
            verify(inventoryRepository, never()).getAvailableQuantity(anyString());
        }
        
        @ParameterizedTest
        @CsvSource({
                "100, 10, true",
                "50, 50, true",
                "10, 20, false",
                "0, 1, false",
                "1, 0, true"
        })
        @DisplayName("다양한 재고와 요청 수량 조합을 처리한다")
        void variousQuantities(int availableQuantity, int requestedQuantity, boolean expected) {
            // Given
            String skuId = "SKU001";
            if (requestedQuantity > 0) {
                when(inventoryRepository.getAvailableQuantity(skuId))
                        .thenReturn(availableQuantity);
            }
            
            // When
            boolean result = stockAvailabilityService.checkSingleOption(skuId, requestedQuantity);
            
            // Then
            assertThat(result).isEqualTo(expected);
            
            if (requestedQuantity > 0) {
                verify(inventoryRepository).getAvailableQuantity(skuId);
            } else {
                verify(inventoryRepository, never()).getAvailableQuantity(anyString());
            }
        }
        
        @ParameterizedTest
        @ValueSource(strings = {"SKU001", "SKU-SPECIAL-001", "sku_test_123", "12345"})
        @DisplayName("다양한 SKU ID 형식을 처리한다")
        void variousSkuFormats(String skuId) {
            // Given
            int requestedQuantity = 10;
            when(inventoryRepository.getAvailableQuantity(skuId))
                    .thenReturn(20);
            
            // When
            boolean result = stockAvailabilityService.checkSingleOption(skuId, requestedQuantity);
            
            // Then
            assertThat(result).isTrue();
            verify(inventoryRepository).getAvailableQuantity(skuId);
        }
        
        @Test
        @DisplayName("대량 요청에 대해서도 정확히 처리한다")
        void largeQuantityRequest() {
            // Given
            String skuId = "SKU001";
            int requestedQuantity = 10000;
            when(inventoryRepository.getAvailableQuantity(skuId))
                    .thenReturn(10001);
            
            // When
            boolean result = stockAvailabilityService.checkSingleOption(skuId, requestedQuantity);
            
            // Then
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("음수 재고량에 대해 IllegalStateException을 던진다")
        void negativeAvailableQuantity() {
            // Given
            String skuId = "SKU001";
            int requestedQuantity = 10;
            when(inventoryRepository.getAvailableQuantity(skuId))
                    .thenReturn(-5);
            
            // When & Then
            assertThatThrownBy(() -> stockAvailabilityService.checkSingleOption(skuId, requestedQuantity))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Available quantity cannot be negative: -5 for SKU: SKU001");
        }
        
        @Test
        @DisplayName("음수 요청 수량에 대해 IllegalArgumentException을 던진다")
        void negativeRequestedQuantity() {
            // Given
            String skuId = "SKU001";
            int requestedQuantity = -10;
            
            // When & Then
            assertThatThrownBy(() -> stockAvailabilityService.checkSingleOption(skuId, requestedQuantity))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Requested quantity cannot be negative: -10 for SKU: SKU001");
            verify(inventoryRepository, never()).getAvailableQuantity(anyString());
        }
    }
    
    @Nested
    @DisplayName("getAvailableQuantity 메서드는")
    class GetAvailableQuantity {
        
        @Test
        @DisplayName("저장소에서 가용 재고량을 조회한다")
        void retrieveFromRepository() {
            // Given
            String skuId = "SKU001";
            int expectedQuantity = 50;
            when(inventoryRepository.getAvailableQuantity(skuId))
                    .thenReturn(expectedQuantity);
            
            // When
            int actualQuantity = stockAvailabilityService.getAvailableQuantity(skuId);
            
            // Then
            assertThat(actualQuantity).isEqualTo(expectedQuantity);
            verify(inventoryRepository).getAvailableQuantity(skuId);
        }
        
        @Test
        @DisplayName("재고가 없을 때 0을 반환한다")
        void noStock() {
            // Given
            String skuId = "SKU001";
            when(inventoryRepository.getAvailableQuantity(skuId))
                    .thenReturn(0);
            
            // When
            int quantity = stockAvailabilityService.getAvailableQuantity(skuId);
            
            // Then
            assertThat(quantity).isZero();
        }
        
        @Test
        @DisplayName("음수 재고량도 그대로 반환한다")
        void negativeStock() {
            // Given
            String skuId = "SKU001";
            when(inventoryRepository.getAvailableQuantity(skuId))
                    .thenReturn(-10);
            
            // When
            int quantity = stockAvailabilityService.getAvailableQuantity(skuId);
            
            // Then
            assertThat(quantity).isEqualTo(-10);
        }
    }
}