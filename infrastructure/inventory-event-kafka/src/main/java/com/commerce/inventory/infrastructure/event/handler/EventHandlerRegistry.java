package com.commerce.inventory.infrastructure.event.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 이벤트 핸들러 레지스트리
 * 이벤트 타입별로 핸들러를 등록하고 관리합니다.
 */
@Component
public class EventHandlerRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(EventHandlerRegistry.class);
    
    private final Map<String, EventHandler> handlers = new ConcurrentHashMap<>();
    
    /**
     * 이벤트 핸들러를 등록합니다.
     * 
     * @param eventType 이벤트 타입
     * @param handler 이벤트 핸들러
     */
    public void register(String eventType, EventHandler handler) {
        if (eventType == null || handler == null) {
            logger.warn("Cannot register null eventType or handler");
            return;
        }
        
        EventHandler previousHandler = handlers.put(eventType, handler);
        if (previousHandler != null) {
            logger.info("Replaced existing handler for event type: {}", eventType);
        } else {
            logger.info("Registered handler for event type: {}", eventType);
        }
    }
    
    /**
     * 이벤트 타입에 대한 핸들러를 반환합니다.
     * 
     * @param eventType 이벤트 타입
     * @return 이벤트 핸들러, 없으면 null
     */
    public EventHandler getHandler(String eventType) {
        if (eventType == null) {
            return null;
        }
        
        EventHandler handler = handlers.get(eventType);
        if (handler == null) {
            logger.debug("No handler registered for event type: {}", eventType);
        }
        return handler;
    }
    
    /**
     * 이벤트 핸들러를 제거합니다.
     * 
     * @param eventType 이벤트 타입
     */
    public void unregister(String eventType) {
        if (eventType == null) {
            return;
        }
        
        EventHandler removed = handlers.remove(eventType);
        if (removed != null) {
            logger.info("Unregistered handler for event type: {}", eventType);
        }
    }
    
    /**
     * 등록된 모든 이벤트 타입을 반환합니다.
     * 
     * @return 이벤트 타입 Set
     */
    public Set<String> getRegisteredEventTypes() {
        return handlers.keySet();
    }
    
    /**
     * 모든 핸들러를 제거합니다.
     */
    public void clear() {
        handlers.clear();
        logger.info("Cleared all event handlers");
    }
}