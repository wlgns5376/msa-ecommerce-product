import { InMemoryProductRepository } from './ProductRepository';
import { Product, UpdateProductDto } from '../domain/Product';

describe('InMemoryProductRepository', () => {
  let repository: InMemoryProductRepository;

  beforeEach(() => {
    repository = new InMemoryProductRepository();
  });

  describe('constructor', () => {
    it('should initialize with given products', () => {
      const products: Product[] = [
        {
          id: '1',
          name: 'Product 1',
          description: 'Description 1',
          price: 100,
          stock: 10,
          categoryId: 'cat-1',
          createdAt: new Date(),
          updatedAt: new Date(),
        },
        {
          id: '2',
          name: 'Product 2',
          description: 'Description 2',
          price: 200,
          stock: 20,
          categoryId: 'cat-2',
          createdAt: new Date(),
          updatedAt: new Date(),
        },
      ];

      const repoWithProducts = new InMemoryProductRepository(products);
      
      expect(repoWithProducts.findById('1')).resolves.toMatchObject({ id: '1', name: 'Product 1' });
      expect(repoWithProducts.findById('2')).resolves.toMatchObject({ id: '2', name: 'Product 2' });
    });
  });

  describe('findById', () => {
    const product: Product = {
      id: '1',
      name: 'Test Product',
      description: 'Test Description',
      price: 100,
      stock: 10,
      categoryId: 'cat-1',
      createdAt: new Date(),
      updatedAt: new Date(),
    };

    beforeEach(() => {
      repository.addProduct(product);
    });

    it('should return product when exists', async () => {
      const result = await repository.findById('1');
      expect(result).toEqual(product);
    });

    it('should return null when product does not exist', async () => {
      const result = await repository.findById('999');
      expect(result).toBeNull();
    });
  });

  describe('update', () => {
    const originalProduct: Product = {
      id: '1',
      name: 'Original Name',
      description: 'Original Description',
      price: 100,
      stock: 10,
      categoryId: 'cat-1',
      createdAt: new Date('2024-01-01'),
      updatedAt: new Date('2024-01-01'),
    };

    beforeEach(() => {
      repository.addProduct(originalProduct);
    });

    it('should update specified fields only', async () => {
      const updateDto: UpdateProductDto = {
        name: 'Updated Name',
        price: 200,
      };

      const result = await repository.update('1', updateDto);

      expect(result).toMatchObject({
        id: '1',
        name: 'Updated Name',
        description: 'Original Description',
        price: 200,
        stock: 10,
        categoryId: 'cat-1',
      });
      expect(result?.updatedAt.getTime()).toBeGreaterThan(originalProduct.updatedAt.getTime());
    });

    it('should update all provided fields', async () => {
      const updateDto: UpdateProductDto = {
        name: 'New Name',
        description: 'New Description',
        price: 300,
        stock: 30,
        categoryId: 'cat-2',
      };

      const result = await repository.update('1', updateDto);

      expect(result).toMatchObject({
        id: '1',
        name: 'New Name',
        description: 'New Description',
        price: 300,
        stock: 30,
        categoryId: 'cat-2',
      });
    });

    it('should not update fields when undefined', async () => {
      const updateDto: UpdateProductDto = {
        name: undefined,
        price: 150,
      };

      const result = await repository.update('1', updateDto);

      expect(result).toMatchObject({
        id: '1',
        name: 'Original Name',
        price: 150,
      });
    });

    it('should return null when product does not exist', async () => {
      const updateDto: UpdateProductDto = {
        name: 'Updated',
      };

      const result = await repository.update('999', updateDto);
      expect(result).toBeNull();
    });
  });

  describe('clear', () => {
    it('should remove all products', async () => {
      const product: Product = {
        id: '1',
        name: 'Test Product',
        description: 'Test',
        price: 100,
        stock: 10,
        categoryId: 'cat-1',
        createdAt: new Date(),
        updatedAt: new Date(),
      };

      repository.addProduct(product);
      expect(await repository.findById('1')).not.toBeNull();

      repository.clear();
      expect(await repository.findById('1')).toBeNull();
    });
  });
});