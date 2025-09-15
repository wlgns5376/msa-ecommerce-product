import { Test, TestingModule } from '@nestjs/testing';
import { ProductService } from './product.service';
import { IProductRepository } from '../domain/product.repository.interface';
import { Product } from '../domain/product.entity';
import { CreateProductDto } from '../presentation/dto/create-product.dto';
import { UpdateProductDto } from '../presentation/dto/update-product.dto';
import { NotFoundException, BadRequestException } from '@nestjs/common';

describe('ProductService', () => {
  let service: ProductService;
  let repository: IProductRepository;

  const mockRepository: Partial<IProductRepository> = {
    save: jest.fn(),
    findById: jest.fn(),
    findBySku: jest.fn(),
    findAll: jest.fn(),
    findByCategory: jest.fn(),
    delete: jest.fn(),
    existsBySku: jest.fn(),
  };

  const mockProduct = new Product(
    '123e4567-e89b-12d3-a456-426614174000',
    'Test Product',
    'Test Description',
    100.00,
    50,
    'TEST-001',
    'Electronics',
    true,
    new Date('2024-01-01'),
    new Date('2024-01-01'),
  );

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        ProductService,
        {
          provide: 'IProductRepository',
          useValue: mockRepository,
        },
      ],
    }).compile();

    service = module.get<ProductService>(ProductService);
    repository = module.get<IProductRepository>('IProductRepository');
    jest.clearAllMocks();
  });

  describe('create', () => {
    it('should create a new product successfully', async () => {
      const createDto: CreateProductDto = {
        name: 'New Product',
        description: 'New Description',
        price: 200.00,
        stock: 30,
        sku: 'NEW-001',
        category: 'Electronics',
      };

      (repository.existsBySku as jest.Mock).mockResolvedValue(false);
      (repository.save as jest.Mock).mockResolvedValue(mockProduct);

      const result = await service.create(createDto);

      expect(result).toBeDefined();
      expect(repository.existsBySku).toHaveBeenCalledWith(createDto.sku);
      expect(repository.save).toHaveBeenCalled();
    });

    it('should throw BadRequestException if SKU already exists', async () => {
      const createDto: CreateProductDto = {
        name: 'New Product',
        description: 'New Description',
        price: 200.00,
        stock: 30,
        sku: 'EXISTING-001',
        category: 'Electronics',
      };

      (repository.existsBySku as jest.Mock).mockResolvedValue(true);

      await expect(service.create(createDto)).rejects.toThrow(BadRequestException);
      expect(repository.save).not.toHaveBeenCalled();
    });

    it('should throw BadRequestException for invalid price', async () => {
      const createDto: CreateProductDto = {
        name: 'New Product',
        description: 'New Description',
        price: -10,
        stock: 30,
        sku: 'NEW-001',
        category: 'Electronics',
      };

      (repository.existsBySku as jest.Mock).mockResolvedValue(false);

      await expect(service.create(createDto)).rejects.toThrow();
    });
  });

  describe('findAll', () => {
    it('should return all products', async () => {
      const products = [mockProduct];
      (repository.findAll as jest.Mock).mockResolvedValue(products);

      const result = await service.findAll();

      expect(result).toEqual(products);
      expect(repository.findAll).toHaveBeenCalled();
    });

    it('should return empty array when no products exist', async () => {
      (repository.findAll as jest.Mock).mockResolvedValue([]);

      const result = await service.findAll();

      expect(result).toEqual([]);
      expect(repository.findAll).toHaveBeenCalled();
    });
  });

  describe('findOne', () => {
    it('should return a product by id', async () => {
      (repository.findById as jest.Mock).mockResolvedValue(mockProduct);

      const result = await service.findOne('123e4567-e89b-12d3-a456-426614174000');

      expect(result).toEqual(mockProduct);
      expect(repository.findById).toHaveBeenCalledWith('123e4567-e89b-12d3-a456-426614174000');
    });

    it('should throw NotFoundException when product not found', async () => {
      (repository.findById as jest.Mock).mockResolvedValue(null);

      await expect(service.findOne('non-existent')).rejects.toThrow(NotFoundException);
    });
  });

  describe('update', () => {
    it('should update a product successfully', async () => {
      const updateDto: UpdateProductDto = {
        name: 'Updated Product',
        price: 150.00,
      };

      const updatedProduct = mockProduct.update(updateDto);

      (repository.findById as jest.Mock).mockResolvedValue(mockProduct);
      (repository.save as jest.Mock).mockResolvedValue(updatedProduct);

      const result = await service.update('123e4567-e89b-12d3-a456-426614174000', updateDto);

      expect(result).toBeDefined();
      expect(repository.findById).toHaveBeenCalledWith('123e4567-e89b-12d3-a456-426614174000');
      expect(repository.save).toHaveBeenCalled();
    });

    it('should throw NotFoundException when product not found', async () => {
      const updateDto: UpdateProductDto = {
        name: 'Updated Product',
      };

      (repository.findById as jest.Mock).mockResolvedValue(null);

      await expect(service.update('non-existent', updateDto)).rejects.toThrow(NotFoundException);
      expect(repository.save).not.toHaveBeenCalled();
    });

    it('should validate SKU uniqueness when updating SKU', async () => {
      const updateDto: UpdateProductDto = {
        sku: 'NEW-SKU',
      };

      const existingProduct = new Product(
        '123',
        'Product',
        'Description',
        100,
        10,
        'OLD-SKU',
        'Category',
      );

      (repository.findById as jest.Mock).mockResolvedValue(existingProduct);
      (repository.findBySku as jest.Mock).mockResolvedValue(mockProduct);

      await expect(service.update('123', updateDto)).rejects.toThrow(BadRequestException);
    });
  });

  describe('remove', () => {
    it('should remove a product successfully', async () => {
      (repository.findById as jest.Mock).mockResolvedValue(mockProduct);
      (repository.delete as jest.Mock).mockResolvedValue(undefined);

      await service.remove('123e4567-e89b-12d3-a456-426614174000');

      expect(repository.findById).toHaveBeenCalledWith('123e4567-e89b-12d3-a456-426614174000');
      expect(repository.delete).toHaveBeenCalledWith('123e4567-e89b-12d3-a456-426614174000');
    });

    it('should throw NotFoundException when product not found', async () => {
      (repository.findById as jest.Mock).mockResolvedValue(null);

      await expect(service.remove('non-existent')).rejects.toThrow(NotFoundException);
      expect(repository.delete).not.toHaveBeenCalled();
    });
  });
});