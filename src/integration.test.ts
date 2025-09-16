import request from 'supertest';
import { createApp } from './app';
import { Application } from 'express';

describe('Integration Test - POST /api/inventory/skus', () => {
  let app: Application;

  beforeEach(() => {
    app = createApp();
  });

  describe('POST /api/inventory/skus', () => {
    it('새로운 SKU를 성공적으로 생성해야 한다', async () => {
      const newSKU = {
        skuCode: 'SKU-INT-001',
        productId: 'PROD-INT-001',
        warehouseId: 'WH-INT-001',
        quantity: 100,
        reservedQuantity: 0,
        minStockLevel: 20,
        maxStockLevel: 500,
      };

      const response = await request(app)
        .post('/api/inventory/skus')
        .send(newSKU)
        .expect(201);

      expect(response.body.success).toBe(true);
      expect(response.body.data).toMatchObject({
        skuCode: 'SKU-INT-001',
        productId: 'PROD-INT-001',
        warehouseId: 'WH-INT-001',
        quantity: 100,
        reservedQuantity: 0,
        availableQuantity: 100,
        minStockLevel: 20,
        maxStockLevel: 500,
        isLowStock: false,
        isOverStock: false,
      });
      expect(response.body.data.id).toBeDefined();
      expect(response.body.data.createdAt).toBeDefined();
      expect(response.body.data.updatedAt).toBeDefined();
    });

    it('예약 수량 없이 SKU를 생성해야 한다', async () => {
      const newSKU = {
        skuCode: 'SKU-INT-002',
        productId: 'PROD-INT-002',
        warehouseId: 'WH-INT-002',
        quantity: 200,
        minStockLevel: 30,
        maxStockLevel: 600,
      };

      const response = await request(app)
        .post('/api/inventory/skus')
        .send(newSKU)
        .expect(201);

      expect(response.body.success).toBe(true);
      expect(response.body.data.reservedQuantity).toBe(0);
      expect(response.body.data.availableQuantity).toBe(200);
    });

    it('중복된 SKU 코드로 생성 시 409 에러를 반환해야 한다', async () => {
      const newSKU = {
        skuCode: 'SKU-DUP-001',
        productId: 'PROD-DUP-001',
        warehouseId: 'WH-DUP-001',
        quantity: 100,
        reservedQuantity: 0,
        minStockLevel: 20,
        maxStockLevel: 500,
      };

      await request(app)
        .post('/api/inventory/skus')
        .send(newSKU)
        .expect(201);

      const response = await request(app)
        .post('/api/inventory/skus')
        .send(newSKU)
        .expect(409);

      expect(response.body.success).toBe(false);
      expect(response.body.error.message).toBe('SKU code already exists');
    });

    it('다른 창고에는 동일한 SKU 코드를 생성할 수 있어야 한다', async () => {
      const sku1 = {
        skuCode: 'SKU-MULTI-001',
        productId: 'PROD-MULTI-001',
        warehouseId: 'WH-001',
        quantity: 100,
        reservedQuantity: 0,
        minStockLevel: 20,
        maxStockLevel: 500,
      };

      const sku2 = {
        skuCode: 'SKU-MULTI-001',
        productId: 'PROD-MULTI-001',
        warehouseId: 'WH-002',
        quantity: 150,
        reservedQuantity: 0,
        minStockLevel: 25,
        maxStockLevel: 550,
      };

      await request(app)
        .post('/api/inventory/skus')
        .send(sku1)
        .expect(201);

      const response = await request(app)
        .post('/api/inventory/skus')
        .send(sku2)
        .expect(201);

      expect(response.body.success).toBe(true);
      expect(response.body.data.warehouseId).toBe('WH-002');
    });

    it('유효성 검증 실패 시 400 에러를 반환해야 한다', async () => {
      const invalidSKU = {
        skuCode: 'SKU-INVALID',
        productId: 'PROD-INVALID',
      };

      const response = await request(app)
        .post('/api/inventory/skus')
        .send(invalidSKU)
        .expect(400);

      expect(response.body.success).toBe(false);
      expect(response.body.error.message).toContain('Validation failed');
      expect(response.body.error.errors).toBeDefined();
      expect(response.body.error.errors.length).toBeGreaterThan(0);
    });

    it('음수 수량으로 생성 시 400 에러를 반환해야 한다', async () => {
      const invalidSKU = {
        skuCode: 'SKU-NEG-001',
        productId: 'PROD-NEG-001',
        warehouseId: 'WH-NEG-001',
        quantity: -100,
        reservedQuantity: 0,
        minStockLevel: 20,
        maxStockLevel: 500,
      };

      const response = await request(app)
        .post('/api/inventory/skus')
        .send(invalidSKU)
        .expect(400);

      expect(response.body.success).toBe(false);
      expect(response.body.error.errors).toContainEqual(
        expect.objectContaining({
          field: 'quantity',
          message: 'Quantity must be non-negative',
        })
      );
    });

    it('재고 부족 상태를 올바르게 표시해야 한다', async () => {
      const lowStockSKU = {
        skuCode: 'SKU-LOW-001',
        productId: 'PROD-LOW-001',
        warehouseId: 'WH-LOW-001',
        quantity: 10,
        reservedQuantity: 0,
        minStockLevel: 20,
        maxStockLevel: 500,
      };

      const response = await request(app)
        .post('/api/inventory/skus')
        .send(lowStockSKU)
        .expect(201);

      expect(response.body.success).toBe(true);
      expect(response.body.data.isLowStock).toBe(true);
      expect(response.body.data.isOverStock).toBe(false);
    });

    it('재고 과다 상태를 올바르게 표시해야 한다', async () => {
      const overStockSKU = {
        skuCode: 'SKU-OVER-001',
        productId: 'PROD-OVER-001',
        warehouseId: 'WH-OVER-001',
        quantity: 600,
        reservedQuantity: 0,
        minStockLevel: 20,
        maxStockLevel: 500,
      };

      const response = await request(app)
        .post('/api/inventory/skus')
        .send(overStockSKU)
        .expect(201);

      expect(response.body.success).toBe(true);
      expect(response.body.data.isLowStock).toBe(false);
      expect(response.body.data.isOverStock).toBe(true);
    });

    it('가용 재고를 올바르게 계산해야 한다', async () => {
      const skuWithReservation = {
        skuCode: 'SKU-RES-001',
        productId: 'PROD-RES-001',
        warehouseId: 'WH-RES-001',
        quantity: 100,
        reservedQuantity: 30,
        minStockLevel: 20,
        maxStockLevel: 500,
      };

      const response = await request(app)
        .post('/api/inventory/skus')
        .send(skuWithReservation)
        .expect(201);

      expect(response.body.success).toBe(true);
      expect(response.body.data.quantity).toBe(100);
      expect(response.body.data.reservedQuantity).toBe(30);
      expect(response.body.data.availableQuantity).toBe(70);
    });
  });

  describe('Health Check', () => {
    it('헬스 체크 엔드포인트가 동작해야 한다', async () => {
      const response = await request(app)
        .get('/health')
        .expect(200);

      expect(response.body.status).toBe('ok');
      expect(response.body.timestamp).toBeDefined();
    });
  });

  describe('404 처리', () => {
    it('존재하지 않는 엔드포인트 접근 시 404를 반환해야 한다', async () => {
      const response = await request(app)
        .get('/api/non-existent')
        .expect(404);

      expect(response.body.success).toBe(false);
      expect(response.body.error.message).toBe('Resource not found');
      expect(response.body.error.path).toBe('/api/non-existent');
    });
  });
});