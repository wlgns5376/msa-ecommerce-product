import { CreateInventorySKUDto } from '../dto/CreateInventorySKUDto';
import { InventorySKU } from '../../domain/inventory/InventorySKU';
import { InventorySKURepository } from '../../infrastructure/repositories/InventorySKURepository';

interface ServiceError extends Error {
  code?: string;
}

export class InventorySKUService {
  constructor(private readonly inventorySKURepository: InventorySKURepository) {}

  public async createSKU(dto: CreateInventorySKUDto): Promise<Record<string, unknown>> {
    const existingSKU = await this.inventorySKURepository.findBySkuCodeAndWarehouse(
      dto.skuCode,
      dto.warehouseId
    );

    if (existingSKU) {
      const error: ServiceError = new Error('SKU code already exists');
      error.code = 'DUPLICATE_SKU';
      throw error;
    }

    try {
      const inventorySKU = new InventorySKU({
        skuCode: dto.skuCode,
        productId: dto.productId,
        warehouseId: dto.warehouseId,
        quantity: dto.quantity,
        reservedQuantity: dto.reservedQuantity ?? 0,
        minStockLevel: dto.minStockLevel,
        maxStockLevel: dto.maxStockLevel,
      });

      const savedSKU = await this.inventorySKURepository.save(inventorySKU);
      return savedSKU.toJSON();
    } catch (error) {
      if (error instanceof Error) {
        const validationError = error as ServiceError;
        validationError.code = 'VALIDATION_ERROR';
        throw validationError;
      }
      throw error;
    }
  }
}