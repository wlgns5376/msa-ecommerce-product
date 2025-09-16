import { v4 as uuidv4 } from 'uuid';

interface InventorySKUProps {
  id?: string;
  skuCode: string;
  productId: string;
  warehouseId: string;
  quantity: number;
  reservedQuantity: number;
  minStockLevel: number;
  maxStockLevel: number;
  createdAt?: Date;
  updatedAt?: Date;
}

export class InventorySKU {
  private readonly _id: string;
  private readonly _skuCode: string;
  private readonly _productId: string;
  private readonly _warehouseId: string;
  private _quantity: number;
  private _reservedQuantity: number;
  private readonly _minStockLevel: number;
  private readonly _maxStockLevel: number;
  private readonly _createdAt: Date;
  private _updatedAt: Date;

  constructor(props: InventorySKUProps) {
    this.validateProps(props);

    this._id = props.id || uuidv4();
    this._skuCode = props.skuCode;
    this._productId = props.productId;
    this._warehouseId = props.warehouseId;
    this._quantity = props.quantity;
    this._reservedQuantity = props.reservedQuantity;
    this._minStockLevel = props.minStockLevel;
    this._maxStockLevel = props.maxStockLevel;
    this._createdAt = props.createdAt || new Date();
    this._updatedAt = props.updatedAt || new Date();
  }

  private validateProps(props: InventorySKUProps): void {
    if (!props.skuCode || props.skuCode.trim().length === 0) {
      throw new Error('SKU code is required');
    }

    if (!props.productId || props.productId.trim().length === 0) {
      throw new Error('Product ID is required');
    }

    if (!props.warehouseId || props.warehouseId.trim().length === 0) {
      throw new Error('Warehouse ID is required');
    }

    if (props.quantity < 0) {
      throw new Error('Quantity cannot be negative');
    }

    if (props.reservedQuantity < 0) {
      throw new Error('Reserved quantity cannot be negative');
    }

    if (props.reservedQuantity > props.quantity) {
      throw new Error('Reserved quantity cannot exceed total quantity');
    }

    if (props.minStockLevel > props.maxStockLevel) {
      throw new Error('Min stock level cannot exceed max stock level');
    }
  }

  get id(): string {
    return this._id;
  }

  get skuCode(): string {
    return this._skuCode;
  }

  get productId(): string {
    return this._productId;
  }

  get warehouseId(): string {
    return this._warehouseId;
  }

  get quantity(): number {
    return this._quantity;
  }

  get reservedQuantity(): number {
    return this._reservedQuantity;
  }

  get availableQuantity(): number {
    return this._quantity - this._reservedQuantity;
  }

  get minStockLevel(): number {
    return this._minStockLevel;
  }

  get maxStockLevel(): number {
    return this._maxStockLevel;
  }

  get createdAt(): Date {
    return this._createdAt;
  }

  get updatedAt(): Date {
    return this._updatedAt;
  }

  public isLowStock(): boolean {
    return this._quantity < this._minStockLevel;
  }

  public isOverStock(): boolean {
    return this._quantity > this._maxStockLevel;
  }

  public addStock(quantity: number): void {
    if (quantity <= 0) {
      throw new Error('Add quantity must be positive');
    }

    this._quantity += quantity;
    this._updatedAt = new Date();
  }

  public removeStock(quantity: number): void {
    if (quantity <= 0) {
      throw new Error('Remove quantity must be positive');
    }

    if (quantity > this.availableQuantity) {
      throw new Error('Insufficient available stock');
    }

    this._quantity -= quantity;
    this._updatedAt = new Date();
  }

  public reserveStock(quantity: number): void {
    if (quantity <= 0) {
      throw new Error('Reserve quantity must be positive');
    }

    if (quantity > this.availableQuantity) {
      throw new Error('Insufficient available stock for reservation');
    }

    this._reservedQuantity += quantity;
    this._updatedAt = new Date();
  }

  public releaseReservation(quantity: number): void {
    if (quantity <= 0) {
      throw new Error('Release quantity must be positive');
    }

    if (quantity > this._reservedQuantity) {
      throw new Error('Cannot release more than reserved quantity');
    }

    this._reservedQuantity -= quantity;
    this._updatedAt = new Date();
  }

  public toJSON(): Record<string, unknown> {
    return {
      id: this._id,
      skuCode: this._skuCode,
      productId: this._productId,
      warehouseId: this._warehouseId,
      quantity: this._quantity,
      reservedQuantity: this._reservedQuantity,
      availableQuantity: this.availableQuantity,
      minStockLevel: this._minStockLevel,
      maxStockLevel: this._maxStockLevel,
      isLowStock: this.isLowStock(),
      isOverStock: this.isOverStock(),
      createdAt: this._createdAt.toISOString(),
      updatedAt: this._updatedAt.toISOString(),
    };
  }
}