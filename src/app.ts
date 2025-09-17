import express from 'express';
import { createProductRouter } from './controller/ProductController';
import { ProductService } from './service/ProductService';
import { InMemoryProductRepository } from './repository/ProductRepository';

export function createApp(): express.Express {
  const app = express();
  
  app.use(express.json());
  
  const productRepository = new InMemoryProductRepository();
  const productService = new ProductService(productRepository);
  const productRouter = createProductRouter(productService);
  
  app.use('/api/products', productRouter);
  
  app.use((err: Error, _req: express.Request, res: express.Response, _next: express.NextFunction): void => {
    if (err instanceof SyntaxError && 'body' in err) {
      res.status(400).json({ error: 'Invalid JSON format' });
      return;
    }
    
    console.error('Unhandled error:', err);
    res.status(500).json({ error: 'Internal server error' });
  });
  
  return app;
}