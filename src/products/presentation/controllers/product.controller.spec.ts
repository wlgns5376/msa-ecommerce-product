import { Test, TestingModule } from '@nestjs/testing';
import { ProductController } from './product.controller';
import { ProductService } from '../../application/product.service';
import { CreateProductDto } from '../dto/create-product.dto';
import { UpdateProductDto } from '../dto/update-product.dto';
import { Product } from '../../domain/product.entity';
import { NotFoundException, BadRequestException } from '@nestjs/common';

describe('ProductController', () => {
  let controller: ProductController;
  let service: ProductService;

  const mockProductService = {
    create: jest.fn(),
    findAll: jest.fn(),
    findOne: jest.fn(),
    update: jest.fn(),
    remove: jest.fn(),
  };

  const mockProduct: Product = {
    id: '123e4567-e89b-12d3-a456-426614174000',
    name: 'Test Product',
    description: 'Test Description',
    price: 100.00,
    stock: 50,
    sku: 'TEST-001',
    category: 'Electronics',
    isActive: true,
    createdAt: new Date('2024-01-01'),
    updatedAt: new Date('2024-01-01'),
  };

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      controllers: [ProductController],
      providers: [
        {
          provide: ProductService,
          useValue: mockProductService,
        },
      ],
    }).compile();

    controller = module.get<ProductController>(ProductController);
    service = module.get<ProductService>(ProductService);
    jest.clearAllMocks();
  });

  describe('create', () => {
    it('should create a new product', async () => {
      const createDto: CreateProductDto = {
        name: 'New Product',
        description: 'New Description',
        price: 200.00,
        stock: 30,
        sku: 'NEW-001',
        category: 'Electronics',
      };

      mockProductService.create.mockResolvedValue(mockProduct);

      const result = await controller.create(createDto);

      expect(result).toEqual(mockProduct);
      expect(service.create).toHaveBeenCalledWith(createDto);
    });

    it('should throw BadRequestException for invalid data', async () => {
      const invalidDto: CreateProductDto = {
        name: '',
        description: 'Description',
        price: -10,
        stock: -5,
        sku: 'INVALID',
        category: 'Electronics',
      };

      mockProductService.create.mockRejectedValue(new BadRequestException('Invalid product data'));

      await expect(controller.create(invalidDto)).rejects.toThrow(BadRequestException);
    });
  });

  describe('findAll', () => {
    it('should return an array of products', async () => {
      const products = [mockProduct];
      mockProductService.findAll.mockResolvedValue(products);

      const result = await controller.findAll();

      expect(result).toEqual(products);
      expect(service.findAll).toHaveBeenCalled();
    });

    it('should return empty array when no products exist', async () => {
      mockProductService.findAll.mockResolvedValue([]);

      const result = await controller.findAll();

      expect(result).toEqual([]);
      expect(service.findAll).toHaveBeenCalled();
    });
  });

  describe('findOne', () => {
    it('should return a single product', async () => {
      mockProductService.findOne.mockResolvedValue(mockProduct);

      const result = await controller.findOne('123e4567-e89b-12d3-a456-426614174000');

      expect(result).toEqual(mockProduct);
      expect(service.findOne).toHaveBeenCalledWith('123e4567-e89b-12d3-a456-426614174000');
    });

    it('should throw NotFoundException when product not found', async () => {
      mockProductService.findOne.mockRejectedValue(new NotFoundException('Product not found'));

      await expect(controller.findOne('non-existent-id')).rejects.toThrow(NotFoundException);
    });
  });

  describe('update', () => {
    it('should update a product', async () => {
      const updateDto: UpdateProductDto = {
        name: 'Updated Product',
        price: 150.00,
      };

      const updatedProduct = { ...mockProduct, ...updateDto };
      mockProductService.update.mockResolvedValue(updatedProduct);

      const result = await controller.update('123e4567-e89b-12d3-a456-426614174000', updateDto);

      expect(result).toEqual(updatedProduct);
      expect(service.update).toHaveBeenCalledWith('123e4567-e89b-12d3-a456-426614174000', updateDto);
    });

    it('should throw NotFoundException when updating non-existent product', async () => {
      const updateDto: UpdateProductDto = {
        name: 'Updated Product',
      };

      mockProductService.update.mockRejectedValue(new NotFoundException('Product not found'));

      await expect(controller.update('non-existent-id', updateDto)).rejects.toThrow(NotFoundException);
    });
  });

  describe('remove', () => {
    it('should remove a product', async () => {
      mockProductService.remove.mockResolvedValue(undefined);

      await controller.remove('123e4567-e89b-12d3-a456-426614174000');

      expect(service.remove).toHaveBeenCalledWith('123e4567-e89b-12d3-a456-426614174000');
    });

    it('should throw NotFoundException when removing non-existent product', async () => {
      mockProductService.remove.mockRejectedValue(new NotFoundException('Product not found'));

      await expect(controller.remove('non-existent-id')).rejects.toThrow(NotFoundException);
    });
  });
});