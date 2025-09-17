import { Product, UpdateProductDto, validateUpdateProductDto } from '../domain/Product';
import { ProductRepository } from '../repository/ProductRepository';

export class ProductNotFoundError extends Error {
  constructor(id: string) {
    super(`Product with id ${id} not found`);
    this.name = 'ProductNotFoundError';
  }
}

export class ProductService {
  constructor(private readonly repository: ProductRepository) {}

  async updateProduct(id: string, dto: UpdateProductDto): Promise<Product> {
    validateUpdateProductDto(dto);

    const existingProduct = await this.repository.findById(id);
    if (!existingProduct) {
      throw new ProductNotFoundError(id);
    }

    const updatedProduct = await this.repository.update(id, dto);
    if (!updatedProduct) {
      throw new ProductNotFoundError(id);
    }

    return updatedProduct;
  }
}