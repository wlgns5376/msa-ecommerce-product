import { InventorySKU } from './InventorySKU';

describe('InventorySKU', () => {
  describe('생성자', () => {
    it('유효한 데이터로 InventorySKU를 생성해야 한다', () => {
      const inventorySku = new InventorySKU({
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: 100,
        reservedQuantity: 10,
        minStockLevel: 20,
        maxStockLevel: 500,
      });

      expect(inventorySku.skuCode).toBe('SKU-001');
      expect(inventorySku.productId).toBe('PROD-001');
      expect(inventorySku.warehouseId).toBe('WH-001');
      expect(inventorySku.quantity).toBe(100);
      expect(inventorySku.reservedQuantity).toBe(10);
      expect(inventorySku.minStockLevel).toBe(20);
      expect(inventorySku.maxStockLevel).toBe(500);
      expect(inventorySku.availableQuantity).toBe(90);
    });

    it('음수 수량으로 생성 시 에러를 발생시켜야 한다', () => {
      expect(() => {
        new InventorySKU({
          skuCode: 'SKU-001',
          productId: 'PROD-001',
          warehouseId: 'WH-001',
          quantity: -10,
          reservedQuantity: 0,
          minStockLevel: 20,
          maxStockLevel: 500,
        });
      }).toThrow('Quantity cannot be negative');
    });

    it('음수 예약 수량으로 생성 시 에러를 발생시켜야 한다', () => {
      expect(() => {
        new InventorySKU({
          skuCode: 'SKU-001',
          productId: 'PROD-001',
          warehouseId: 'WH-001',
          quantity: 100,
          reservedQuantity: -10,
          minStockLevel: 20,
          maxStockLevel: 500,
        });
      }).toThrow('Reserved quantity cannot be negative');
    });

    it('예약 수량이 전체 수량보다 클 때 에러를 발생시켜야 한다', () => {
      expect(() => {
        new InventorySKU({
          skuCode: 'SKU-001',
          productId: 'PROD-001',
          warehouseId: 'WH-001',
          quantity: 50,
          reservedQuantity: 60,
          minStockLevel: 20,
          maxStockLevel: 500,
        });
      }).toThrow('Reserved quantity cannot exceed total quantity');
    });

    it('최소 재고 수준이 최대 재고 수준보다 클 때 에러를 발생시켜야 한다', () => {
      expect(() => {
        new InventorySKU({
          skuCode: 'SKU-001',
          productId: 'PROD-001',
          warehouseId: 'WH-001',
          quantity: 100,
          reservedQuantity: 10,
          minStockLevel: 600,
          maxStockLevel: 500,
        });
      }).toThrow('Min stock level cannot exceed max stock level');
    });

    it('빈 SKU 코드로 생성 시 에러를 발생시켜야 한다', () => {
      expect(() => {
        new InventorySKU({
          skuCode: '',
          productId: 'PROD-001',
          warehouseId: 'WH-001',
          quantity: 100,
          reservedQuantity: 10,
          minStockLevel: 20,
          maxStockLevel: 500,
        });
      }).toThrow('SKU code is required');
    });

    it('빈 제품 ID로 생성 시 에러를 발생시켜야 한다', () => {
      expect(() => {
        new InventorySKU({
          skuCode: 'SKU-001',
          productId: '',
          warehouseId: 'WH-001',
          quantity: 100,
          reservedQuantity: 10,
          minStockLevel: 20,
          maxStockLevel: 500,
        });
      }).toThrow('Product ID is required');
    });

    it('빈 창고 ID로 생성 시 에러를 발생시켜야 한다', () => {
      expect(() => {
        new InventorySKU({
          skuCode: 'SKU-001',
          productId: 'PROD-001',
          warehouseId: '',
          quantity: 100,
          reservedQuantity: 10,
          minStockLevel: 20,
          maxStockLevel: 500,
        });
      }).toThrow('Warehouse ID is required');
    });
  });

  describe('재고 상태 확인', () => {
    it('재고가 부족한 상태를 올바르게 판단해야 한다', () => {
      const inventorySku = new InventorySKU({
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: 15,
        reservedQuantity: 0,
        minStockLevel: 20,
        maxStockLevel: 500,
      });

      expect(inventorySku.isLowStock()).toBe(true);
    });

    it('재고가 충분한 상태를 올바르게 판단해야 한다', () => {
      const inventorySku = new InventorySKU({
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: 100,
        reservedQuantity: 0,
        minStockLevel: 20,
        maxStockLevel: 500,
      });

      expect(inventorySku.isLowStock()).toBe(false);
    });

    it('재고가 과다한 상태를 올바르게 판단해야 한다', () => {
      const inventorySku = new InventorySKU({
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: 600,
        reservedQuantity: 0,
        minStockLevel: 20,
        maxStockLevel: 500,
      });

      expect(inventorySku.isOverStock()).toBe(true);
    });

    it('정상 재고 상태를 올바르게 판단해야 한다', () => {
      const inventorySku = new InventorySKU({
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: 300,
        reservedQuantity: 0,
        minStockLevel: 20,
        maxStockLevel: 500,
      });

      expect(inventorySku.isOverStock()).toBe(false);
    });
  });

  describe('재고 업데이트', () => {
    it('재고 수량을 증가시킬 수 있어야 한다', () => {
      const inventorySku = new InventorySKU({
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: 100,
        reservedQuantity: 10,
        minStockLevel: 20,
        maxStockLevel: 500,
      });

      inventorySku.addStock(50);

      expect(inventorySku.quantity).toBe(150);
      expect(inventorySku.availableQuantity).toBe(140);
    });

    it('음수로 재고 증가 시도 시 에러를 발생시켜야 한다', () => {
      const inventorySku = new InventorySKU({
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: 100,
        reservedQuantity: 10,
        minStockLevel: 20,
        maxStockLevel: 500,
      });

      expect(() => {
        inventorySku.addStock(-10);
      }).toThrow('Add quantity must be positive');
    });

    it('재고 수량을 감소시킬 수 있어야 한다', () => {
      const inventorySku = new InventorySKU({
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: 100,
        reservedQuantity: 10,
        minStockLevel: 20,
        maxStockLevel: 500,
      });

      inventorySku.removeStock(30);

      expect(inventorySku.quantity).toBe(70);
      expect(inventorySku.availableQuantity).toBe(60);
    });

    it('가용 재고보다 많이 감소시키려 할 때 에러를 발생시켜야 한다', () => {
      const inventorySku = new InventorySKU({
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: 100,
        reservedQuantity: 80,
        minStockLevel: 20,
        maxStockLevel: 500,
      });

      expect(() => {
        inventorySku.removeStock(30);
      }).toThrow('Insufficient available stock');
    });
  });

  describe('재고 예약', () => {
    it('재고를 예약할 수 있어야 한다', () => {
      const inventorySku = new InventorySKU({
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: 100,
        reservedQuantity: 10,
        minStockLevel: 20,
        maxStockLevel: 500,
      });

      inventorySku.reserveStock(20);

      expect(inventorySku.reservedQuantity).toBe(30);
      expect(inventorySku.availableQuantity).toBe(70);
    });

    it('가용 재고보다 많이 예약하려 할 때 에러를 발생시켜야 한다', () => {
      const inventorySku = new InventorySKU({
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: 100,
        reservedQuantity: 80,
        minStockLevel: 20,
        maxStockLevel: 500,
      });

      expect(() => {
        inventorySku.reserveStock(30);
      }).toThrow('Insufficient available stock for reservation');
    });

    it('예약을 해제할 수 있어야 한다', () => {
      const inventorySku = new InventorySKU({
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: 100,
        reservedQuantity: 30,
        minStockLevel: 20,
        maxStockLevel: 500,
      });

      inventorySku.releaseReservation(20);

      expect(inventorySku.reservedQuantity).toBe(10);
      expect(inventorySku.availableQuantity).toBe(90);
    });

    it('예약된 재고보다 많이 해제하려 할 때 에러를 발생시켜야 한다', () => {
      const inventorySku = new InventorySKU({
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: 100,
        reservedQuantity: 10,
        minStockLevel: 20,
        maxStockLevel: 500,
      });

      expect(() => {
        inventorySku.releaseReservation(20);
      }).toThrow('Cannot release more than reserved quantity');
    });
  });

  describe('toJSON', () => {
    it('객체를 JSON으로 올바르게 직렬화해야 한다', () => {
      const inventorySku = new InventorySKU({
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: 100,
        reservedQuantity: 10,
        minStockLevel: 20,
        maxStockLevel: 500,
      });

      const json = inventorySku.toJSON();

      expect(json).toEqual({
        id: expect.any(String),
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: 100,
        reservedQuantity: 10,
        availableQuantity: 90,
        minStockLevel: 20,
        maxStockLevel: 500,
        isLowStock: false,
        isOverStock: false,
        createdAt: expect.any(String),
        updatedAt: expect.any(String),
      });
    });
  });
});