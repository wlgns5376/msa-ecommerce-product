import request from 'supertest';
import express from 'express';
import { createProductRouter } from './ProductController';
import { InMemoryProductRepository } from '../repository/ProductRepository';
import { ProductService } from '../service/ProductService';
import { Product } from '../domain/Product';

describe('ProductController - PUT /api/products/:id', () => {
  let app: express.Express;
  let repository: InMemoryProductRepository;

  beforeEach(() => {
    repository = new InMemoryProductRepository();
    const service = new ProductService(repository);
    
    app = express();
    app.use(express.json());
    app.use('/api/products', createProductRouter(service));
    
    app.use((err: Error, _req: express.Request, res: express.Response, _next: express.NextFunction): void => {
      if (err instanceof SyntaxError && 'body' in err) {
        res.status(400).json({ error: 'Invalid JSON format' });
        return;
      }
      
      console.error('Unhandled error:', err);
      res.status(500).json({ error: 'Internal server error' });
    });
  });

  afterEach(() => {
    repository.clear();
  });

  describe('Given a product exists', () => {
    const existingProduct: Product = {
      id: '1',
      name: 'Test Product',
      description: 'Test Description',
      price: 100,
      stock: 10,
      categoryId: 'cat-1',
      createdAt: new Date('2024-01-01'),
      updatedAt: new Date('2024-01-01'),
    };

    beforeEach(() => {
      repository.addProduct(existingProduct);
    });

    describe('When updating with valid data', () => {
      it('Then should return 200 with updated product', async () => {
        const updateData = {
          name: 'Updated Product',
          price: 150,
          stock: 20,
        };

        const response = await request(app)
          .put('/api/products/1')
          .send(updateData)
          .expect(200);

        expect(response.body).toMatchObject({
          id: '1',
          name: 'Updated Product',
          description: 'Test Description',
          price: 150,
          stock: 20,
          categoryId: 'cat-1',
        });
        
        expect(new Date(response.body.updatedAt).getTime())
          .toBeGreaterThan(existingProduct.updatedAt.getTime());
      });

      it('Then should update only provided fields', async () => {
        const updateData = {
          description: 'New Description',
        };

        const response = await request(app)
          .put('/api/products/1')
          .send(updateData)
          .expect(200);

        expect(response.body).toMatchObject({
          id: '1',
          name: 'Test Product',
          description: 'New Description',
          price: 100,
          stock: 10,
          categoryId: 'cat-1',
        });
      });
    });

    describe('When updating with invalid data', () => {
      it('Then should return 400 for empty name', async () => {
        const updateData = {
          name: '   ',
        };

        const response = await request(app)
          .put('/api/products/1')
          .send(updateData)
          .expect(400);

        expect(response.body).toMatchObject({
          error: 'Product name cannot be empty',
        });
      });

      it('Then should return 400 for negative price', async () => {
        const updateData = {
          price: -10,
        };

        const response = await request(app)
          .put('/api/products/1')
          .send(updateData)
          .expect(400);

        expect(response.body).toMatchObject({
          error: 'Product price cannot be negative',
        });
      });

      it('Then should return 400 for negative stock', async () => {
        const updateData = {
          stock: -5,
        };

        const response = await request(app)
          .put('/api/products/1')
          .send(updateData)
          .expect(400);

        expect(response.body).toMatchObject({
          error: 'Product stock cannot be negative',
        });
      });

      it('Then should return 400 for non-integer stock', async () => {
        const updateData = {
          stock: 10.5,
        };

        const response = await request(app)
          .put('/api/products/1')
          .send(updateData)
          .expect(400);

        expect(response.body).toMatchObject({
          error: 'Product stock must be an integer',
        });
      });

      it('Then should return 400 for empty categoryId', async () => {
        const updateData = {
          categoryId: '  ',
        };

        const response = await request(app)
          .put('/api/products/1')
          .send(updateData)
          .expect(400);

        expect(response.body).toMatchObject({
          error: 'Category ID cannot be empty',
        });
      });
    });
  });

  describe('Given a product does not exist', () => {
    describe('When trying to update', () => {
      it('Then should return 404', async () => {
        const updateData = {
          name: 'New Name',
        };

        const response = await request(app)
          .put('/api/products/999')
          .send(updateData)
          .expect(404);

        expect(response.body).toMatchObject({
          error: 'Product with id 999 not found',
        });
      });
    });
  });

  describe('When sending empty update data', () => {
    const existingProduct: Product = {
      id: '1',
      name: 'Test Product',
      description: 'Test Description',
      price: 100,
      stock: 10,
      categoryId: 'cat-1',
      createdAt: new Date('2024-01-01'),
      updatedAt: new Date('2024-01-01'),
    };

    beforeEach(() => {
      repository.addProduct(existingProduct);
    });

    it('Then should return 200 with unchanged product except updatedAt', async () => {
      const response = await request(app)
        .put('/api/products/1')
        .send({})
        .expect(200);

      expect(response.body).toMatchObject({
        id: '1',
        name: 'Test Product',
        description: 'Test Description',
        price: 100,
        stock: 10,
        categoryId: 'cat-1',
      });
      
      expect(new Date(response.body.updatedAt).getTime())
        .toBeGreaterThan(existingProduct.updatedAt.getTime());
    });
  });

  describe('When sending malformed JSON', () => {
    it('Then should return 400', async () => {
      const response = await request(app)
        .put('/api/products/1')
        .set('Content-Type', 'application/json')
        .send('{"invalid json}')
        .expect(400);

      expect(response.body).toHaveProperty('error');
    });
  });

  describe('When internal error occurs', () => {
    it('Then should return 500', async () => {
      jest.spyOn(repository, 'findById').mockRejectedValueOnce(new Error('Database error'));

      const response = await request(app)
        .put('/api/products/1')
        .send({ name: 'Test' })
        .expect(500);

      expect(response.body).toEqual({ error: 'Internal server error' });
    });
  });
});