package com.commerce.inventory.infrastructure.event.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * 이벤트 핸들러 자동 등록 설정
 */
@Configuration
public class EventHandlerConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(EventHandlerConfiguration.class);
    
    private final EventHandlerRegistry registry;
    private final List<EventHandler> handlers;
    
    public EventHandlerConfiguration(EventHandlerRegistry registry, List<EventHandler> handlers) {
        this.registry = registry;
        this.handlers = handlers;
    }
    
    @PostConstruct
    public void registerHandlers() {
        logger.info("Registering {} event handlers", handlers.size());
        
        for (EventHandler handler : handlers) {
            String eventType = handler.getEventType();
            registry.register(eventType, handler);
            logger.info("Registered handler for event type: {}", eventType);
        }
        
        logger.info("Event handler registration completed. Registered types: {}", 
                   registry.getRegisteredEventTypes());
    }
}