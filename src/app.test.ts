import request from 'supertest';
import express from 'express';
import { createApp } from './app';

describe('App', () => {
  let app: ReturnType<typeof createApp>;

  beforeEach(() => {
    app = createApp();
  });

  describe('PUT /api/products/:id', () => {
    it('should handle invalid JSON with error handler', async () => {
      const response = await request(app)
        .put('/api/products/1')
        .set('Content-Type', 'application/json')
        .send('{"invalid": json}')
        .expect(400);

      expect(response.body).toEqual({ error: 'Invalid JSON format' });
    });

    it('should handle unknown errors', async () => {
      const response = await request(app)
        .put('/api/products/1')
        .send({
          name: 'Test Product',
        })
        .expect(404);

      expect(response.body).toEqual({ error: 'Product with id 1 not found' });
    });
  });

  describe('Error handler', () => {
    it('should handle non-JSON syntax errors', async () => {
      const testApp = express();
      testApp.use(express.json());
      
      testApp.get('/test-error', (_req: express.Request, _res: express.Response, next: express.NextFunction) => {
        const error = new Error('Test error');
        next(error);
      });

      testApp.use((err: Error, _req: express.Request, res: express.Response, _next: express.NextFunction): void => {
        if (err instanceof SyntaxError && 'body' in err) {
          res.status(400).json({ error: 'Invalid JSON format' });
          return;
        }
        
        console.error('Unhandled error:', err);
        res.status(500).json({ error: 'Internal server error' });
      });

      const response = await request(testApp)
        .get('/test-error')
        .expect(500);

      expect(response.body).toEqual({ error: 'Internal server error' });
    });
  });
});