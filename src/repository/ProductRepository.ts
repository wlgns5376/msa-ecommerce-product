import { Product, UpdateProductDto } from '../domain/Product';

export interface ProductRepository {
  findById(id: string): Promise<Product | null>;
  update(id: string, dto: UpdateProductDto): Promise<Product | null>;
}

export class InMemoryProductRepository implements ProductRepository {
  private products: Map<string, Product> = new Map();

  constructor(initialProducts: Product[] = []) {
    initialProducts.forEach(product => {
      this.products.set(product.id, product);
    });
  }

  async findById(id: string): Promise<Product | null> {
    return this.products.get(id) || null;
  }

  async update(id: string, dto: UpdateProductDto): Promise<Product | null> {
    const existingProduct = this.products.get(id);
    if (!existingProduct) {
      return null;
    }

    const updatedProduct: Product = {
      ...existingProduct,
      ...(dto.name !== undefined && { name: dto.name }),
      ...(dto.description !== undefined && { description: dto.description }),
      ...(dto.price !== undefined && { price: dto.price }),
      ...(dto.stock !== undefined && { stock: dto.stock }),
      ...(dto.categoryId !== undefined && { categoryId: dto.categoryId }),
      updatedAt: new Date(),
    };

    this.products.set(id, updatedProduct);
    return updatedProduct;
  }

  clear(): void {
    this.products.clear();
  }

  addProduct(product: Product): void {
    this.products.set(product.id, product);
  }
}