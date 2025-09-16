export interface CreateInventorySKUDto {
  skuCode: string;
  productId: string;
  warehouseId: string;
  quantity: number;
  reservedQuantity?: number;
  minStockLevel: number;
  maxStockLevel: number;
}