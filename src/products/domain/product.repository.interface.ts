import { Product } from './product.entity';

export interface IProductRepository {
  save(product: Product): Promise<Product>;
  findById(id: string): Promise<Product | null>;
  findBySku(sku: string): Promise<Product | null>;
  findAll(): Promise<Product[]>;
  findByCategory(category: string): Promise<Product[]>;
  delete(id: string): Promise<void>;
  existsBySku(sku: string): Promise<boolean>;
}