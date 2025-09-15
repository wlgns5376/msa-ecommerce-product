import { Injectable } from '@nestjs/common';
import { IProductRepository } from '../../domain/product.repository.interface';
import { Product } from '../../domain/product.entity';

@Injectable()
export class InMemoryProductRepository implements IProductRepository {
  private products: Map<string, Product> = new Map();

  async save(product: Product): Promise<Product> {
    this.products.set(product.id, product);
    return product;
  }

  async findById(id: string): Promise<Product | null> {
    return this.products.get(id) || null;
  }

  async findBySku(sku: string): Promise<Product | null> {
    const products = Array.from(this.products.values());
    return products.find(p => p.sku === sku) || null;
  }

  async findAll(): Promise<Product[]> {
    return Array.from(this.products.values());
  }

  async findByCategory(category: string): Promise<Product[]> {
    const products = Array.from(this.products.values());
    return products.filter(p => p.category === category);
  }

  async delete(id: string): Promise<void> {
    this.products.delete(id);
  }

  async existsBySku(sku: string): Promise<boolean> {
    const product = await this.findBySku(sku);
    return product !== null;
  }

  clear(): void {
    this.products.clear();
  }
}