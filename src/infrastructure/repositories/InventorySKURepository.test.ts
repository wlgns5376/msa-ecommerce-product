import { InventorySKURepository } from './InventorySKURepository';
import { InventorySKU } from '../../domain/inventory/InventorySKU';

describe('InventorySKURepository', () => {
  let repository: InventorySKURepository;

  beforeEach(() => {
    repository = new InventorySKURepository();
  });

  describe('save', () => {
    it('SKU를 저장하고 반환해야 한다', async () => {
      const inventorySKU = new InventorySKU({
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: 100,
        reservedQuantity: 10,
        minStockLevel: 20,
        maxStockLevel: 500,
      });

      const savedSKU = await repository.save(inventorySKU);

      expect(savedSKU).toEqual(inventorySKU);
      expect(savedSKU.skuCode).toBe('SKU-001');
      expect(savedSKU.productId).toBe('PROD-001');
      expect(savedSKU.warehouseId).toBe('WH-001');
    });

    it('동일한 ID로 업데이트해야 한다', async () => {
      const inventorySKU = new InventorySKU({
        id: 'test-id',
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: 100,
        reservedQuantity: 10,
        minStockLevel: 20,
        maxStockLevel: 500,
      });

      await repository.save(inventorySKU);

      inventorySKU.addStock(50);

      const updatedSKU = await repository.save(inventorySKU);

      expect(updatedSKU.quantity).toBe(150);
      expect(updatedSKU.id).toBe('test-id');
    });
  });

  describe('findById', () => {
    it('저장된 SKU를 ID로 찾을 수 있어야 한다', async () => {
      const inventorySKU = new InventorySKU({
        id: 'test-id',
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: 100,
        reservedQuantity: 10,
        minStockLevel: 20,
        maxStockLevel: 500,
      });

      await repository.save(inventorySKU);

      const foundSKU = await repository.findById('test-id');

      expect(foundSKU).not.toBeNull();
      expect(foundSKU?.id).toBe('test-id');
      expect(foundSKU?.skuCode).toBe('SKU-001');
    });

    it('존재하지 않는 ID로 조회 시 null을 반환해야 한다', async () => {
      const foundSKU = await repository.findById('non-existent-id');

      expect(foundSKU).toBeNull();
    });
  });

  describe('findBySkuCode', () => {
    it('SKU 코드로 SKU를 찾을 수 있어야 한다', async () => {
      const inventorySKU = new InventorySKU({
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: 100,
        reservedQuantity: 10,
        minStockLevel: 20,
        maxStockLevel: 500,
      });

      await repository.save(inventorySKU);

      const foundSKU = await repository.findBySkuCode('SKU-001');

      expect(foundSKU).not.toBeNull();
      expect(foundSKU?.skuCode).toBe('SKU-001');
    });

    it('존재하지 않는 SKU 코드로 조회 시 null을 반환해야 한다', async () => {
      const foundSKU = await repository.findBySkuCode('NON-EXISTENT');

      expect(foundSKU).toBeNull();
    });
  });

  describe('findBySkuCodeAndWarehouse', () => {
    it('SKU 코드와 창고 ID로 SKU를 찾을 수 있어야 한다', async () => {
      const inventorySKU = new InventorySKU({
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: 100,
        reservedQuantity: 10,
        minStockLevel: 20,
        maxStockLevel: 500,
      });

      await repository.save(inventorySKU);

      const foundSKU = await repository.findBySkuCodeAndWarehouse('SKU-001', 'WH-001');

      expect(foundSKU).not.toBeNull();
      expect(foundSKU?.skuCode).toBe('SKU-001');
      expect(foundSKU?.warehouseId).toBe('WH-001');
    });

    it('다른 창고의 동일한 SKU 코드는 찾지 않아야 한다', async () => {
      const inventorySKU = new InventorySKU({
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: 100,
        reservedQuantity: 10,
        minStockLevel: 20,
        maxStockLevel: 500,
      });

      await repository.save(inventorySKU);

      const foundSKU = await repository.findBySkuCodeAndWarehouse('SKU-001', 'WH-002');

      expect(foundSKU).toBeNull();
    });

    it('존재하지 않는 조합으로 조회 시 null을 반환해야 한다', async () => {
      const foundSKU = await repository.findBySkuCodeAndWarehouse('NON-EXISTENT', 'WH-001');

      expect(foundSKU).toBeNull();
    });
  });

  describe('findByWarehouse', () => {
    it('특정 창고의 모든 SKU를 찾을 수 있어야 한다', async () => {
      const sku1 = new InventorySKU({
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: 100,
        reservedQuantity: 10,
        minStockLevel: 20,
        maxStockLevel: 500,
      });

      const sku2 = new InventorySKU({
        skuCode: 'SKU-002',
        productId: 'PROD-002',
        warehouseId: 'WH-001',
        quantity: 200,
        reservedQuantity: 20,
        minStockLevel: 30,
        maxStockLevel: 600,
      });

      const sku3 = new InventorySKU({
        skuCode: 'SKU-003',
        productId: 'PROD-003',
        warehouseId: 'WH-002',
        quantity: 150,
        reservedQuantity: 15,
        minStockLevel: 25,
        maxStockLevel: 550,
      });

      await repository.save(sku1);
      await repository.save(sku2);
      await repository.save(sku3);

      const warehouseSKUs = await repository.findByWarehouse('WH-001');

      expect(warehouseSKUs).toHaveLength(2);
      expect(warehouseSKUs.map(s => s.skuCode)).toEqual(['SKU-001', 'SKU-002']);
    });

    it('존재하지 않는 창고로 조회 시 빈 배열을 반환해야 한다', async () => {
      const warehouseSKUs = await repository.findByWarehouse('NON-EXISTENT');

      expect(warehouseSKUs).toEqual([]);
    });
  });

  describe('findByProductId', () => {
    it('특정 제품의 모든 SKU를 찾을 수 있어야 한다', async () => {
      const sku1 = new InventorySKU({
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: 100,
        reservedQuantity: 10,
        minStockLevel: 20,
        maxStockLevel: 500,
      });

      const sku2 = new InventorySKU({
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-002',
        quantity: 200,
        reservedQuantity: 20,
        minStockLevel: 30,
        maxStockLevel: 600,
      });

      const sku3 = new InventorySKU({
        skuCode: 'SKU-002',
        productId: 'PROD-002',
        warehouseId: 'WH-001',
        quantity: 150,
        reservedQuantity: 15,
        minStockLevel: 25,
        maxStockLevel: 550,
      });

      await repository.save(sku1);
      await repository.save(sku2);
      await repository.save(sku3);

      const productSKUs = await repository.findByProductId('PROD-001');

      expect(productSKUs).toHaveLength(2);
      expect(productSKUs.map(s => s.warehouseId)).toEqual(['WH-001', 'WH-002']);
    });

    it('존재하지 않는 제품으로 조회 시 빈 배열을 반환해야 한다', async () => {
      const productSKUs = await repository.findByProductId('NON-EXISTENT');

      expect(productSKUs).toEqual([]);
    });
  });

  describe('delete', () => {
    it('SKU를 삭제할 수 있어야 한다', async () => {
      const inventorySKU = new InventorySKU({
        id: 'test-id',
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: 100,
        reservedQuantity: 10,
        minStockLevel: 20,
        maxStockLevel: 500,
      });

      await repository.save(inventorySKU);
      await repository.delete('test-id');

      const foundSKU = await repository.findById('test-id');

      expect(foundSKU).toBeNull();
    });

    it('존재하지 않는 ID로 삭제 시 에러를 발생시키지 않아야 한다', async () => {
      await expect(repository.delete('non-existent-id')).resolves.not.toThrow();
    });
  });

  describe('findAll', () => {
    it('모든 SKU를 반환해야 한다', async () => {
      const sku1 = new InventorySKU({
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: 100,
        reservedQuantity: 10,
        minStockLevel: 20,
        maxStockLevel: 500,
      });

      const sku2 = new InventorySKU({
        skuCode: 'SKU-002',
        productId: 'PROD-002',
        warehouseId: 'WH-002',
        quantity: 200,
        reservedQuantity: 20,
        minStockLevel: 30,
        maxStockLevel: 600,
      });

      await repository.save(sku1);
      await repository.save(sku2);

      const allSKUs = await repository.findAll();

      expect(allSKUs).toHaveLength(2);
      expect(allSKUs.map(s => s.skuCode)).toEqual(['SKU-001', 'SKU-002']);
    });

    it('SKU가 없을 때 빈 배열을 반환해야 한다', async () => {
      const allSKUs = await repository.findAll();

      expect(allSKUs).toEqual([]);
    });
  });
});