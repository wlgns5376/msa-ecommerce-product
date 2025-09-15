import { Injectable, NotFoundException, BadRequestException, Inject } from '@nestjs/common';
import { IProductRepository } from '../domain/product.repository.interface';
import { Product } from '../domain/product.entity';
import { CreateProductDto } from '../presentation/dto/create-product.dto';
import { UpdateProductDto } from '../presentation/dto/update-product.dto';

@Injectable()
export class ProductService {
  constructor(
    @Inject('IProductRepository')
    private readonly productRepository: IProductRepository,
  ) {}

  async create(createProductDto: CreateProductDto): Promise<Product> {
    const skuExists = await this.productRepository.existsBySku(createProductDto.sku);
    if (skuExists) {
      throw new BadRequestException(`Product with SKU ${createProductDto.sku} already exists`);
    }

    try {
      const product = Product.create(
        createProductDto.name,
        createProductDto.description,
        createProductDto.price,
        createProductDto.stock,
        createProductDto.sku,
        createProductDto.category,
        createProductDto.isActive ?? true,
      );

      return await this.productRepository.save(product);
    } catch (error) {
      if (error instanceof Error) {
        throw new BadRequestException(error.message);
      }
      throw error;
    }
  }

  async findAll(): Promise<Product[]> {
    return await this.productRepository.findAll();
  }

  async findOne(id: string): Promise<Product> {
    const product = await this.productRepository.findById(id);
    if (!product) {
      throw new NotFoundException(`Product with ID ${id} not found`);
    }
    return product;
  }

  async update(id: string, updateProductDto: UpdateProductDto): Promise<Product> {
    const product = await this.findOne(id);

    if (updateProductDto.sku && updateProductDto.sku !== product.sku) {
      const existingProduct = await this.productRepository.findBySku(updateProductDto.sku);
      if (existingProduct && existingProduct.id !== id) {
        throw new BadRequestException(`Product with SKU ${updateProductDto.sku} already exists`);
      }
    }

    try {
      const updatedProduct = product.update(updateProductDto);
      return await this.productRepository.save(updatedProduct);
    } catch (error) {
      if (error instanceof Error) {
        throw new BadRequestException(error.message);
      }
      throw error;
    }
  }

  async remove(id: string): Promise<void> {
    await this.findOne(id);
    await this.productRepository.delete(id);
  }

  async findByCategory(category: string): Promise<Product[]> {
    return await this.productRepository.findByCategory(category);
  }

  async findBySku(sku: string): Promise<Product> {
    const product = await this.productRepository.findBySku(sku);
    if (!product) {
      throw new NotFoundException(`Product with SKU ${sku} not found`);
    }
    return product;
  }
}