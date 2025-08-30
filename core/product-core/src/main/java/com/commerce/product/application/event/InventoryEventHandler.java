package com.commerce.product.application.event;

import com.commerce.product.application.usecase.UpdateProductUseCase;
import com.commerce.product.domain.event.ProductOutOfStockEvent;
import com.commerce.product.domain.event.ProductInStockEvent;
import com.commerce.product.domain.model.ProductId;
import com.commerce.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 재고 관련 이벤트를 처리하는 핸들러
 * Inventory 서비스로부터 받은 이벤트를 처리하여 상품 상태를 업데이트합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventHandler {

    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 재고 부족 이벤트를 처리합니다.
     * SKU의 재고가 0이 되었을 때 연관된 상품을 품절 상태로 변경합니다.
     * 
     * @param skuId 재고가 부족한 SKU ID
     */
    @Transactional
    public void handleStockDepletedEvent(String skuId) {
        log.info("Processing stock depleted event for SKU: {}", skuId);
        
        try {
            // SKU와 연관된 모든 상품을 찾습니다
            productRepository.findProductsBySkuId(skuId).forEach(product -> {
                if (!product.isOutOfStock()) {
                    product.markAsOutOfStock();
                    productRepository.save(product);
                    
                    // 도메인 이벤트 발행
                    product.getDomainEvents().forEach(eventPublisher::publishEvent);
                    
                    log.info("Product {} marked as out of stock due to SKU {} depletion", 
                            product.getId().value(), skuId);
                }
            });
        } catch (Exception e) {
            log.error("Error handling stock depleted event for SKU: {}", skuId, e);
            throw e;
        }
    }

    /**
     * 재고 복원 이벤트를 처리합니다.
     * SKU의 재고가 다시 확보되었을 때 연관된 상품을 재고 있음 상태로 변경합니다.
     * 
     * @param skuId 재고가 복원된 SKU ID
     */
    @Transactional
    public void handleStockReplenishedEvent(String skuId) {
        log.info("Processing stock replenished event for SKU: {}", skuId);
        
        try {
            // SKU와 연관된 모든 상품을 찾습니다
            productRepository.findProductsBySkuId(skuId).forEach(product -> {
                if (product.isOutOfStock()) {
                    // 번들 상품의 경우 모든 SKU의 재고를 확인해야 합니다
                    boolean allSkusAvailable = product.getOptions().stream()
                            .allMatch(option -> {
                                if (option.isBundle()) {
                                    return option.getSkuMapping().mappings().keySet().stream()
                                            .allMatch(this::isSkuAvailable);
                                } else {
                                    return isSkuAvailable(option.getSingleSkuId());
                                }
                            });
                    
                    if (allSkusAvailable) {
                        product.markAsInStock();
                        productRepository.save(product);
                        
                        // 도메인 이벤트 발행
                        product.getDomainEvents().forEach(eventPublisher::publishEvent);
                        
                        log.info("Product {} marked as in stock due to SKU {} replenishment", 
                                product.getId().value(), skuId);
                    }
                }
            });
        } catch (Exception e) {
            log.error("Error handling stock replenished event for SKU: {}", skuId, e);
            throw e;
        }
    }

    /**
     * SKU의 재고 가용성을 확인합니다.
     * 실제 구현에서는 Inventory 서비스를 호출하여 확인해야 합니다.
     */
    private boolean isSkuAvailable(String skuId) {
        // TODO: Inventory 서비스를 통해 실제 재고 확인
        // 현재는 임시로 true 반환
        return true;
    }
}