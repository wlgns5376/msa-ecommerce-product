import { ProductService, ProductNotFoundError } from './ProductService';
import { InMemoryProductRepository } from '../repository/ProductRepository';
import { Product, ProductValidationError, UpdateProductDto } from '../domain/Product';

describe('ProductService', () => {
  let service: ProductService;
  let repository: InMemoryProductRepository;

  beforeEach(() => {
    repository = new InMemoryProductRepository();
    service = new ProductService(repository);
  });

  describe('updateProduct', () => {
    const existingProduct: Product = {
      id: '1',
      name: 'Original Product',
      description: 'Original Description',
      price: 100,
      stock: 10,
      categoryId: 'cat-1',
      createdAt: new Date('2024-01-01'),
      updatedAt: new Date('2024-01-01'),
    };

    beforeEach(() => {
      repository.addProduct(existingProduct);
    });

    describe('Given valid product and data', () => {
      it('Then should update product successfully', async () => {
        const updateDto: UpdateProductDto = {
          name: 'Updated Product',
          price: 200,
        };

        const result = await service.updateProduct('1', updateDto);

        expect(result).toMatchObject({
          id: '1',
          name: 'Updated Product',
          description: 'Original Description',
          price: 200,
          stock: 10,
          categoryId: 'cat-1',
        });
        expect(result.updatedAt.getTime()).toBeGreaterThan(existingProduct.updatedAt.getTime());
      });
    });

    describe('Given non-existent product', () => {
      it('Then should throw ProductNotFoundError', async () => {
        const updateDto: UpdateProductDto = {
          name: 'Updated',
        };

        await expect(service.updateProduct('999', updateDto))
          .rejects
          .toThrow(new ProductNotFoundError('999'));
      });
    });

    describe('Given invalid update data', () => {
      it('Then should throw ProductValidationError for empty name', async () => {
        const updateDto: UpdateProductDto = {
          name: '  ',
        };

        await expect(service.updateProduct('1', updateDto))
          .rejects
          .toThrow(new ProductValidationError('Product name cannot be empty'));
      });

      it('Then should throw ProductValidationError for negative price', async () => {
        const updateDto: UpdateProductDto = {
          price: -50,
        };

        await expect(service.updateProduct('1', updateDto))
          .rejects
          .toThrow(new ProductValidationError('Product price cannot be negative'));
      });
    });

    describe('When repository returns null unexpectedly', () => {
      it('Then should throw ProductNotFoundError', async () => {
        jest.spyOn(repository, 'update').mockResolvedValueOnce(null);

        const updateDto: UpdateProductDto = {
          name: 'Updated',
        };

        await expect(service.updateProduct('1', updateDto))
          .rejects
          .toThrow(new ProductNotFoundError('1'));
      });
    });
  });

  describe('ProductNotFoundError', () => {
    it('should have correct message and name', () => {
      const error = new ProductNotFoundError('123');
      expect(error.name).toBe('ProductNotFoundError');
      expect(error.message).toBe('Product with id 123 not found');
    });
  });
});