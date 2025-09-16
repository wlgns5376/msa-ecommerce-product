import { InventorySKU } from '../../domain/inventory/InventorySKU';

export class InventorySKURepository {
  private storage: Map<string, InventorySKU> = new Map();

  public async save(inventorySKU: InventorySKU): Promise<InventorySKU> {
    this.storage.set(inventorySKU.id, inventorySKU);
    return inventorySKU;
  }

  public async findById(id: string): Promise<InventorySKU | null> {
    return this.storage.get(id) || null;
  }

  public async findBySkuCode(skuCode: string): Promise<InventorySKU | null> {
    for (const sku of this.storage.values()) {
      if (sku.skuCode === skuCode) {
        return sku;
      }
    }
    return null;
  }

  public async findBySkuCodeAndWarehouse(
    skuCode: string,
    warehouseId: string
  ): Promise<InventorySKU | null> {
    for (const sku of this.storage.values()) {
      if (sku.skuCode === skuCode && sku.warehouseId === warehouseId) {
        return sku;
      }
    }
    return null;
  }

  public async findByWarehouse(warehouseId: string): Promise<InventorySKU[]> {
    const results: InventorySKU[] = [];
    for (const sku of this.storage.values()) {
      if (sku.warehouseId === warehouseId) {
        results.push(sku);
      }
    }
    return results;
  }

  public async findByProductId(productId: string): Promise<InventorySKU[]> {
    const results: InventorySKU[] = [];
    for (const sku of this.storage.values()) {
      if (sku.productId === productId) {
        results.push(sku);
      }
    }
    return results;
  }

  public async delete(id: string): Promise<void> {
    this.storage.delete(id);
  }

  public async findAll(): Promise<InventorySKU[]> {
    return Array.from(this.storage.values());
  }
}