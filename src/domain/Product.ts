export interface Product {
  readonly id: string;
  readonly name: string;
  readonly description: string;
  readonly price: number;
  readonly stock: number;
  readonly categoryId: string;
  readonly createdAt: Date;
  readonly updatedAt: Date;
}

export interface UpdateProductDto {
  readonly name?: string;
  readonly description?: string;
  readonly price?: number;
  readonly stock?: number;
  readonly categoryId?: string;
}

export class ProductValidationError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'ProductValidationError';
  }
}

export function validateUpdateProductDto(dto: UpdateProductDto): void {
  if (dto.name !== undefined && dto.name.trim().length === 0) {
    throw new ProductValidationError('Product name cannot be empty');
  }

  if (dto.price !== undefined) {
    if (dto.price < 0) {
      throw new ProductValidationError('Product price cannot be negative');
    }
    if (!Number.isFinite(dto.price)) {
      throw new ProductValidationError('Product price must be a valid number');
    }
  }

  if (dto.stock !== undefined) {
    if (dto.stock < 0) {
      throw new ProductValidationError('Product stock cannot be negative');
    }
    if (!Number.isInteger(dto.stock)) {
      throw new ProductValidationError('Product stock must be an integer');
    }
  }

  if (dto.categoryId !== undefined && dto.categoryId.trim().length === 0) {
    throw new ProductValidationError('Category ID cannot be empty');
  }
}