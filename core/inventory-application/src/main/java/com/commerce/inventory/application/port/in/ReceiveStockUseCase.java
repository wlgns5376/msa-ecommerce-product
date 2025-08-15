package com.commerce.inventory.application.port.in;

public interface ReceiveStockUseCase {
    void receive(ReceiveStockCommand command);
}